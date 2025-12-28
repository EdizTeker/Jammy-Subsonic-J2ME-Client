import javax.microedition.lcdui.*;
import java.util.Vector;
import cc.nnproject.json.*;

public class ArtistList extends List implements CommandListener {
    private MainMIDlet mainApp;
    private int currentOffset;
    private Command backCommand;

    private Vector loadedArtistIds = new Vector();

    public ArtistList(MainMIDlet app, int offset) {
        super("Artists (Loading...)", List.IMPLICIT);
        this.mainApp = app;
        this.currentOffset = offset;

        backCommand = new Command("Back", Command.BACK, 0);
        addCommand(backCommand);
        setCommandListener(this);

        loadArtists();
    }

    public void loadPage(int offset) {
        this.currentOffset = offset;
        deleteAll();
        loadedArtistIds.removeAllElements();
        setTitle("Loading...");
        loadArtists();
    }

    private void loadArtists() {
        new Thread(new Runnable() {
            public void run() {
                loadedArtistIds.removeAllElements();

                if (currentOffset >= 10) {
                    Display.getDisplay(mainApp).callSerially(new Runnable() {
                        public void run() {
                            loadedArtistIds.addElement("PREV_PAGE_BTN");
                            append("<< Prev Page", null);
                        }
                    });
                }

                String apiCall = mainApp.buildApiUrl("search3.view") +
                        "&query=&artistCount=10&artistOffset=" + currentOffset +
                        "&albumCount=0&songCount=0";

                String json = NetworkHelper.performRequest(apiCall);

                if (json == null) {
                    showError("Network Error");
                    return;
                }

                try {
                    JSONObject root = JSON.getObject(json);
                    JSONObject sub = root.getObject("subsonic-response");
                    JSONObject search = sub.getObject("searchResult3");

                    if (search.has("artist")) {
                        final JSONArray artists = search.getArray("artist");
                        int count = artists.size();

                        for (int i = 0; i < count; i++) {
                            JSONObject artistObj = artists.getObject(i);
                            final String name = artistObj.getString("name", "Unknown");
                            final String id = artistObj.getString("id", "");

                            Display.getDisplay(mainApp).callSerially(new Runnable() {
                                public void run() {
                                    loadedArtistIds.addElement(id);
                                    append(name, null);

                                    int visible = loadedArtistIds.size();
                                    if(loadedArtistIds.contains("PREV_PAGE_BTN")) visible--;
                                    int total = currentOffset + visible;
                                    setTitle("Artists (" + currentOffset + "-" + total + ")");
                                }
                            });
                        }

                        if (count == 10) {
                            Display.getDisplay(mainApp).callSerially(new Runnable() {
                                public void run() {
                                    loadedArtistIds.addElement("NEXT_PAGE_BTN");
                                    append("Next Page >>", null);
                                }
                            });
                        }
                    } else {
                        showError("No artists found.");
                    }

                } catch (Exception e) {
                    showError("Error: " + e.getMessage());
                }
            }
        }).start();
    }

    private void showError(final String msg) {
        Display.getDisplay(mainApp).callSerially(new Runnable() {
            public void run() { append(msg, null); }
        });
    }

    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            mainApp.showMainMenu();
        } else if (c == List.SELECT_COMMAND) {
            int index = getSelectedIndex();
            if (index >= 0 && index < loadedArtistIds.size()) {
                String id = (String) loadedArtistIds.elementAt(index);

                if (id.equals("NEXT_PAGE_BTN")) {
                    loadPage(currentOffset + 10);
                } else if (id.equals("PREV_PAGE_BTN")) {
                    int newOffset = currentOffset - 10;
                    if (newOffset < 0) newOffset = 0;
                    loadPage(newOffset);
                } else {
                    mainApp.showArtistAlbums(id);
                }
            }
        }
    }
}
