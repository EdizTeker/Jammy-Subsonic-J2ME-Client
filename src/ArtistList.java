import javax.microedition.lcdui.*;
import java.util.Vector;
import cc.nnproject.json.*;

public class ArtistList extends List implements CommandListener {
    private MainMIDlet mainApp;
    private int currentOffset;
    private Command backCommand;
    private String currentQuery = "";

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

                if (currentOffset >= 20) {
                    addToUI("PREV_PAGE_BTN", "<< Prev Page", null);
                }

                // search3.view
                String encodedQuery = NetworkHelper.urlEncode(currentQuery);
                String apiCall = mainApp.buildApiUrl("search3.view") +
                        "&query=" + encodedQuery +
                        "&artistCount=20&artistOffset=" + currentOffset +
                        "&albumCount=0&songCount=0";

                String json = NetworkHelper.performRequest(apiCall);

                if (json == null) { showError("Network Error"); return; }

                try {
                    JSONObject root = JSON.getObject(json);
                    if (!root.has("subsonic-response")) { showError("Bad Response"); return; }
                    JSONObject sub = root.getObject("subsonic-response");

                    boolean foundArtists = false;

                    if (sub.has("searchResult3")) {
                        JSONObject search = sub.getObject("searchResult3");
                        if (search.has("artist")) {
                            JSONArray artists = search.getArray("artist");
                            int count = artists.size();

                            if (count > 0) {
                                foundArtists = true;
                                parseArtistArray(artists);

                                // Add Next Page Button if we got a full page
                                if (count == 20) {
                                    addToUI("NEXT_PAGE_BTN", "Next Page >>", null);
                                }
                            }
                        }
                    }

                    // FALLBACK
                    if (!foundArtists && currentQuery.length() == 0 && currentOffset == 0) {
                        loadFromIndexes();
                    } else if (!foundArtists) {
                        showError("No artists found.");
                    }

                } catch (Exception e) {
                    showError("Error: " + e.getMessage());
                }
            }
        }).start();
    }

    // getIndexes.view
    private void loadFromIndexes() {
        Display.getDisplay(mainApp).callSerially(new Runnable() {
            public void run() { setTitle("Loading Index..."); }
        });

        String apiCall = mainApp.buildApiUrl("getIndexes.view") + "&ifModifiedSince=0";
        String json = NetworkHelper.performRequest(apiCall);

        if (json == null) return;

        try {
            JSONObject root = JSON.getObject(json);
            JSONObject sub = root.getObject("subsonic-response");

            if (sub.has("indexes")) {
                JSONObject indexesObj = sub.getObject("indexes");
                if (indexesObj.has("index")) {
                    JSONArray indexList = indexesObj.getArray("index");

                    for (int i = 0; i < indexList.size(); i++) {
                        JSONObject letterGroup = indexList.getObject(i);
                        if (letterGroup.has("artist")) {
                            JSONArray artists = letterGroup.getArray("artist");
                            parseArtistArray(artists);

                            if (loadedArtistIds.size() >= 100) break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            showError("Index Error: " + e.getMessage());
        }
    }

    private void parseArtistArray(JSONArray artists) {
        for (int i = 0; i < artists.size(); i++) {
            JSONObject artistObj = artists.getObject(i);
            String name = artistObj.getString("name", "Unknown");
            String id = artistObj.getString("id", "");
            addToUI(id, name, null);
        }
    }

    private void addToUI(final String id, final String name, final Image img) {
        Display.getDisplay(mainApp).callSerially(new Runnable() {
            public void run() {
                loadedArtistIds.addElement(id);
                append(name, img);

                int count = loadedArtistIds.size();
                if(loadedArtistIds.contains("PREV_PAGE_BTN")) count--;
                if(loadedArtistIds.contains("NEXT_PAGE_BTN")) count--;

                if (currentQuery.length() > 0) {
                    setTitle("Results (" + count + ")");
                } else if (currentOffset > 0) {
                    setTitle("Artists (" + currentOffset + "-" + (currentOffset+count) + ")");
                } else {
                    setTitle("Artists (" + count + ")");
                }
            }
        });
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
