import javax.microedition.lcdui.*;
import java.util.Vector;
import cc.nnproject.json.*;

public class AlbumList extends List implements CommandListener {
    private MainMIDlet mainApp;
    private int currentOffset;
    private Command backCommand;
    private String artistId = null;

    private Vector loadedAlbumIds = new Vector();

    // For all albums.
    public AlbumList(MainMIDlet app, int offset) {
        super("Albums", List.IMPLICIT);
        this.mainApp = app;
        this.currentOffset = offset;
        init();
        loadAlbums();
    }

    // For artist albums.
    public AlbumList(MainMIDlet app, String artistId) {
        super("Artist Albums", List.IMPLICIT);
        this.mainApp = app;
        this.currentOffset = 0;
        this.artistId = artistId;
        init();
        loadAlbums();
    }

    private void init() {
        backCommand = new Command("Back", Command.BACK, 0);
        addCommand(backCommand);
        setCommandListener(this);
    }

    private void loadAlbums() {
        new Thread(new Runnable() {
            public void run() {
                loadedAlbumIds.removeAllElements();

                String apiCall;
                if (artistId != null) {
                    apiCall = mainApp.buildApiUrl("getArtist.view") + "&id=" + artistId;
                } else {
                    if (currentOffset >= 10) {
                        Display.getDisplay(mainApp).callSerially(new Runnable() {
                            public void run() {
                                loadedAlbumIds.addElement("PREV_PAGE_BTN");
                                append("<< Prev Page", null);
                            }
                        });
                    }
                    apiCall = mainApp.buildApiUrl("getAlbumList2.view") +
                            "&type=alphabeticalByName&size=10&offset=" + currentOffset;
                }

                String json = NetworkHelper.performRequest(apiCall);
                if (json == null) { return; }

                try {
                    JSONObject root = JSON.getObject(json);
                    JSONObject sub = root.getObject("subsonic-response");

                    JSONArray albums = null;

                    if (artistId != null) {
                        if (sub.has("artist")) {
                            JSONObject artistObj = sub.getObject("artist");
                            if (artistObj.has("album")) {
                                albums = artistObj.getArray("album");
                            }
                        }
                    } else {
                        if (sub.has("albumList2")) {
                            JSONObject listObj = sub.getObject("albumList2");
                            if (listObj.has("album")) {
                                albums = listObj.getArray("album");
                            }
                        }
                    }

                    if (albums != null) {
                        final int count = albums.size();
                        for (int i = 0; i < count; i++) {
                            JSONObject album = albums.getObject(i);
                            final String name = album.getString("name", "Unknown");
                            final String artist = album.getString("artist", "");
                            final String id = album.getString("id", "");
                            String coverId = album.getString("coverArt", "");

                            Image coverImage = null;

                            if (Config.loadAlbumArt.equals("1") && coverId.length() > 0) {
                                String coverUrl = mainApp.buildApiUrl("getCoverArt.view") + "&id=" + coverId + "&size=50";
                                coverImage = NetworkHelper.downloadImage(coverUrl, coverId);
                            }

                            final Image finalImg = coverImage;

                            Display.getDisplay(mainApp).callSerially(new Runnable() {
                                public void run() {
                                    loadedAlbumIds.addElement(id);
                                    append(name + "\n" + artist, finalImg);

                                    if (artistId == null) {
                                        int vCount = loadedAlbumIds.size();
                                        if (loadedAlbumIds.contains("PREV_PAGE_BTN")) vCount--;
                                        setTitle("Albums (" + currentOffset + "-" + (currentOffset + vCount) + ")");
                                    } else {
                                        setTitle("Albums (" + loadedAlbumIds.size() + ")");
                                    }
                                }
                            });
                        }

                        if (artistId == null && count == 10) {
                            Display.getDisplay(mainApp).callSerially(new Runnable() {
                                public void run() {
                                    loadedAlbumIds.addElement("NEXT_PAGE_BTN");
                                    append("Next Page >>", null);
                                }
                            });
                        }

                    } else { }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            if (artistId != null) {
                mainApp.showArtistPage(0);
            } else {
                mainApp.showMainMenu();
            }
        } else if (c == List.SELECT_COMMAND) {
            int index = getSelectedIndex();

            if (index >= 0 && index < loadedAlbumIds.size()) {
                String id = (String) loadedAlbumIds.elementAt(index);

                if (id.equals("NEXT_PAGE_BTN")) {
                    mainApp.showAlbumPage(currentOffset + 10);
                } else if (id.equals("PREV_PAGE_BTN")) {
                    int newOffset = currentOffset - 10;
                    if (newOffset < 0) newOffset = 0;
                    mainApp.showAlbumPage(newOffset);
                } else {
                    mainApp.showSongList(id);
                }
            }
        }
    }
}
