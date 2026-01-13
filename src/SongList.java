import javax.microedition.lcdui.*;
import java.util.Vector;
import cc.nnproject.json.*;

public class SongList extends List implements CommandListener {
    private MainMIDlet mainApp;
    private String albumId;
    private Command backCommand;

    private Vector songListItems = new Vector(30);

    public SongList(MainMIDlet app, String albumId) {
        super("Loading Tracks...", List.IMPLICIT);
        this.mainApp = app;
        this.albumId = albumId;

        backCommand = new Command("Back", Command.BACK, 0);
        addCommand(backCommand);
        setCommandListener(this);

        loadSongs();
    }

    private void loadSongs() {
        new Thread(new Runnable() {
            public void run() {
                String apiCall = mainApp.buildApiUrl("getAlbum.view") + "&id=" + albumId;

                String json = NetworkHelper.performRequest(apiCall);

                if (json == null) {
                    showError("Network Error");
                    return;
                }

                try {
                    JSONObject root = JSON.getObject(json);
                    JSONObject sub = root.getObject("subsonic-response");

                    if (sub.has("album")) {
                        JSONObject albumObj = sub.getObject("album");

                        final String albumName = albumObj.getString("name", "Album");

                        Display.getDisplay(mainApp).callSerially(new Runnable() {
                            public void run() { setTitle(albumName); }
                        });

                        if (albumObj.has("song")) {
                            JSONArray songs = albumObj.getArray("song");

                            for (int i = 0; i < songs.size(); i++) {
                                JSONObject song = songs.getObject(i);

                                String t = song.getString("title", "?");
                                String a = song.getString("artist", "?");
                                String id = song.getString("id", "");
                                String an = song.getString("album", "");
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


                                int trackNum = 0;
                                try { trackNum = song.getInt("track"); } catch(Exception e){}

                                final String label = (trackNum > 0 ? trackNum + ". " : "") + t;

                                final SongItem item = new SongItem(id, t, a, an, albId, artId, isStarred, fmt, szStr, br);

                                Display.getDisplay(mainApp).callSerially(new Runnable() {
                                    public void run() {
                                        songListItems.addElement(item);
                                        append(label, null);
                                    }
                                });
                            }
                        } else {
                            showError("Empty Album");
                        }
                    } else {
                        showError("Album not found");
                    }
                } catch (Exception e) {
                    showError("Error: " + e.getMessage());
                    e.printStackTrace();
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
            mainApp.showAlbumPage(0);
        } else if (c == List.SELECT_COMMAND) {
            int index = getSelectedIndex();
            if (index >= 0 && index < songListItems.size()) {
                mainApp.showPlayer(songListItems, index);
            }
        }
    }
}