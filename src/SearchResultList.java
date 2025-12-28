import javax.microedition.lcdui.*;
import java.util.Vector;
import cc.nnproject.json.*;

public class SearchResultList extends List implements CommandListener {
    public static final int TYPE_SONG = 0;
    public static final int TYPE_ALBUM = 1;
    public static final int TYPE_ARTIST = 2;

    private MainMIDlet mainApp;
    private String query;
    private int searchType;
    private Command backCommand;

    // We store data here. If song, it holds songitems. If album/artist, holds string ids.
    private Vector resultItems = new Vector();

    public SearchResultList(MainMIDlet app, String query, int type) {
        super("Results: " + query, List.IMPLICIT);
        this.mainApp = app;
        this.query = query;
        this.searchType = type;

        backCommand = new Command("Back", Command.BACK, 0);
        addCommand(backCommand);
        setCommandListener(this);

        doSearch();
    }

    private void doSearch() {
        new Thread(new Runnable() {
            public void run() {
                String params = "&query=" + NetworkHelper.urlEncode(query);

                if (searchType == TYPE_SONG) {
                    params += "&songCount=20&albumCount=0&artistCount=0";
                } else if (searchType == TYPE_ALBUM) {
                    params += "&songCount=0&albumCount=20&artistCount=0";
                } else if (searchType == TYPE_ARTIST) {
                    params += "&songCount=0&albumCount=0&artistCount=20";
                }

                String apiCall = mainApp.buildApiUrl("search3.view") + params;
                String json = NetworkHelper.performRequest(apiCall);

                if (json == null) { showError("Network Error"); return; }

                try {
                    JSONObject root = JSON.getObject(json);
                    JSONObject sub = root.getObject("subsonic-response");
                    if (!sub.has("searchResult3")) { showError("No results"); return; }

                    JSONObject res = sub.getObject("searchResult3");

                    // 2. Parse based on Type
                    if (searchType == TYPE_SONG) parseSongs(res);
                    else if (searchType == TYPE_ALBUM) parseAlbums(res);
                    else if (searchType == TYPE_ARTIST) parseArtists(res);

                } catch (Exception e) {
                    showError("Error: " + e.getMessage());
                }
            }
        }).start();
    }

    private void parseSongs(JSONObject res) {
        if (!res.has("song")) { showError("No songs found"); return; }
        JSONArray list = res.getArray("song");
        for (int i=0; i<list.size(); i++) {
            JSONObject o = list.getObject(i);
            String t = o.getString("title", "?");
            String a = o.getString("artist", "?");
            String id = o.getString("id", "");
            String an = o.getString("album", "");
            String artId = o.getString("artistId", "");
            String albId = o.getString("albumId", "");

            boolean isStarred = o.has("starred");

            String fmt = o.getString("suffix", "mp3"); // e.g. "mp3", "flac"

            int brInt = 0;
            try { brInt = o.getInt("bitRate"); } catch (Exception e) {}
            String br = String.valueOf(brInt);

            long sizeBytes = 0;
            try { sizeBytes = o.getLong("size"); } catch(Exception e){}

            String szStr;
            if (sizeBytes > 1024 * 1024) {
                long mb = sizeBytes / (1024 * 1024);
                long decimal = (sizeBytes % (1024 * 1024)) / 104857;
                szStr = mb + "." + decimal + " MB";
            } else {
                szStr = (sizeBytes / 1024) + " KB";
            }

            final SongItem item = new SongItem(id, t, a, an, albId, artId, isStarred, fmt, szStr, br);

            addToUI(t + "\n" + a, item);
        }
    }

    private void parseAlbums(JSONObject res) {
        if (!res.has("album")) { showError("No albums found"); return; }
        JSONArray list = res.getArray("album");
        for (int i=0; i<list.size(); i++) {
            JSONObject o = list.getObject(i);
            String n = o.getString("name", "?");
            String a = o.getString("artist", "?");
            final String id = o.getString("id", "");

            addToUI(n + "\n" + a, id);
        }
    }

    private void parseArtists(JSONObject res) {
        if (!res.has("artist")) { showError("No artists found"); return; }
        JSONArray list = res.getArray("artist");
        for (int i=0; i<list.size(); i++) {
            JSONObject o = list.getObject(i);
            String n = o.getString("name", "?");
            final String id = o.getString("id", "");

            addToUI(n, id);
        }
    }

    private void addToUI(final String label, final Object item) {
        Display.getDisplay(mainApp).callSerially(new Runnable() {
            public void run() {
                append(label, null);
                resultItems.addElement(item);
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
            if (index < 0 || index >= resultItems.size()) return;

            Object item = resultItems.elementAt(index);

            if (searchType == TYPE_SONG) {
                mainApp.showPlayer(resultItems, index);
            }
            else if (searchType == TYPE_ALBUM) {
                mainApp.showSongList((String)item);
            }
            else if (searchType == TYPE_ARTIST) {
                mainApp.showArtistAlbums((String)item);
            }
        }
    }
}
