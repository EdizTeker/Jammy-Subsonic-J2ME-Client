import javax.microedition.lcdui.*;
import java.util.Vector;
import cc.nnproject.json.*;

public class FavoritesList extends List implements CommandListener {
    private MainMIDlet mainApp;
    private Command backCommand;
    private Vector songListItems = new Vector();

    public FavoritesList(MainMIDlet app) {
        super("Favorites", List.IMPLICIT);
        this.mainApp = app;

        backCommand = new Command("Back", Command.BACK, 0);
        addCommand(backCommand);
        setCommandListener(this);

        loadFavorites();
    }

    private void loadFavorites() {
        new Thread(new Runnable() {
            public void run() {
                String apiCall = mainApp.buildApiUrl("getStarred.view");
                String json = NetworkHelper.performRequest(apiCall);

                if (json == null) {
                    showError("Network Error");
                    return;
                }

                try {
                    JSONObject root = JSON.getObject(json);
                    JSONObject sub = root.getObject("subsonic-response");

                    if (sub.has("starred")) {
                        JSONObject starred = sub.getObject("starred");

                        if (starred.has("song")) {
                            JSONArray songs = starred.getArray("song");

                            for (int i = 0; i < songs.size(); i++) {
                                JSONObject song = songs.getObject(i);

                                String t = song.getString("title", "?");
                                String a = song.getString("artist", "?");
                                String id = song.getString("id", "");
                                String artId = song.getString("artistId", "");
                                String albId = song.getString("albumId", "");

                                boolean isStarred = song.has("starred");

                                String fmt = song.getString("suffix", "mp3");

                                int brInt = 0;
                                try { brInt = song.getInt("bitRate"); } catch (Exception e) {}
                                String br = String.valueOf(brInt);

                                long sizeBytes = 0;
                                try { sizeBytes = song.getLong("size"); } catch(Exception e){}

                                String szStr;
                                if (sizeBytes > 1024 * 1024) {
                                    long mb = sizeBytes / (1024 * 1024);
                                    long decimal = (sizeBytes % (1024 * 1024)) / 104857;
                                    szStr = mb + "." + decimal + " MB";
                                } else {
                                    szStr = (sizeBytes / 1024) + " KB";
                                }

                                final SongItem item = new SongItem(id, t, a, "fav", albId, artId, isStarred, fmt, szStr, br);

                                Display.getDisplay(mainApp).callSerially(new Runnable() {
                                    public void run() {
                                        songListItems.addElement(item);
                                        append(item.title + " - " + item.artist, null);
                                    }
                                });
                            }
                        } else {
                            showError("No favorites found.");
                        }
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
            songListItems.removeAllElements();
            System.gc();
            mainApp.showMainMenu();
        } else if (c == List.SELECT_COMMAND) {
            int index = getSelectedIndex();
            if (index >= 0 && index < songListItems.size()) {
                mainApp.showPlayer(songListItems, index);
            }
        }
    }
}
