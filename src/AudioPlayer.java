import javax.microedition.media.*;
import javax.microedition.lcdui.Display;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.media.control.VolumeControl;
import java.io.InputStream;
import java.io.IOException;

public class AudioPlayer implements PlayerListener {
    private Player player;
    private MainMIDlet mainApp;
    private PlaybackListener listener;
    private int storedVolume = 80;

    private HttpConnection httpConn;
    private InputStream inputStream;

    private final Object lock = new Object();
    private String currentUrl = "";
    public boolean isPlaying = false;

    public AudioPlayer(MainMIDlet app) {
        this.mainApp = app;
    }

    public void setListener(PlaybackListener l) {
        this.listener = l;
    }

    public void play(final String url) {
        synchronized(lock) {
            currentUrl = url;
        }

        stop();
        System.gc();

        new Thread(new Runnable() {
            public void run() {
                try {
                    synchronized(lock) {
                        if (!url.equals(currentUrl)) return;
                    }

                    // Direct URL.
                    try {
                        System.out.println("Trying Method A: Direct URL...");
                        Player p = Manager.createPlayer(url);
                        finalizePlayer(p);
                        return;
                    } catch (MediaException me) {
                        System.out.println("Direct play failed (Format unsupported?): " + me.getMessage());
                    } catch (Exception e) {
                        System.out.println("Method A failed.");
                    }

                    // Manual Stream.
                    HttpConnection tempConn = (HttpConnection) Connector.open(url);
                    int responseCode = tempConn.getResponseCode();

                    if (responseCode != HttpConnection.HTTP_OK) {
                        System.out.println("Server Error: " + responseCode);
                        tempConn.close();
                        return;
                    }

                    InputStream tempStream = tempConn.openInputStream();
                    String contentType = "audio/mpeg";
                    if (Config.format.equals("raw")) {
                        String serverType = tempConn.getType();
                        if (serverType != null && serverType.length() > 0) {
                            contentType = serverType;
                            System.out.println("Auto-detected format: " + contentType);
                        }
                    }
                    if (Config.format.equals("wav")) contentType = "audio/x-wav";
                    else if (Config.format.equals("aac")) contentType = "audio/aac";

                    Player p = null;
                    try {
                        p = Manager.createPlayer(tempStream, contentType);
                    } catch (MediaException me) {
                        if (listener != null) {
                            listener.onError("Format not supported: " + contentType + "\nTry changing format in settings.");
                        }
                        tempStream.close();
                        tempConn.close();
                        return;
                    }

                    synchronized(lock) {
                        if (!url.equals(currentUrl)) {
                            p.close();
                            tempStream.close();
                            tempConn.close();
                            return;
                        }
                        httpConn = tempConn;
                        inputStream = tempStream;
                    }

                    finalizePlayer(p);

                } catch (Exception e) {
                    System.out.println("Critical Error: " + e.getMessage());
                    e.printStackTrace();
                    stop();
                }
            }
        }).start();
    }

    private void finalizePlayer(Player p) throws MediaException {
        synchronized(lock) {
            player = p;
            player.addPlayerListener(AudioPlayer.this);
        }

        player.realize();
        player.prefetch();

        setVolume(storedVolume);

        player.start();
        isPlaying = true;
    }

    public void stop() {
        synchronized(lock) {
            if (player != null) {
                try {
                    player.stop();
                    player.close();
                } catch (Exception e) { }
                player = null;
            }
            if (inputStream != null) {
                try { inputStream.close(); } catch (Exception e) {}
                inputStream = null;
            }
            if (httpConn != null) {
                try { httpConn.close(); } catch (Exception e) {}
                httpConn = null;
            }
            isPlaying = false;
        }
    }

    public void pause() {
        synchronized(lock) {
            if (player != null && isPlaying) {
                try {
                    player.stop();
                    isPlaying = false;
                } catch (Exception e) { }
            }
        }
    }

    public void resume() {
        synchronized(lock) {
            if (player != null && !isPlaying) {
                try {
                    player.start();
                    isPlaying = true;
                } catch (Exception e) { }
            }
        }
    }

    public void setVolume(int level) {
        this.storedVolume = level;
        if (player != null) {
            try {
                VolumeControl vc = (VolumeControl) player.getControl("VolumeControl");
                if (vc != null) {
                    vc.setLevel(storedVolume);
                }
            } catch (Exception e) {}
        }
    }

    public long getMediaTime() {
        synchronized(lock) {
            if (player != null) {
                return player.getMediaTime();
            }
        }
        return -1;
    }

    public long getDuration() {
        if (player != null) {
            try { return player.getDuration(); }
            catch (Exception e) { return -1; }
        }
        return -1;
    }

    public void playerUpdate(Player p, String event, Object eventData) {
        if (event.equals(PlayerListener.END_OF_MEDIA)) {
            isPlaying = false;
            if (listener != null) {
                listener.onSongEnd();
            }
        }
    }

    public interface PlaybackListener {
        void onSongEnd();
        void onError(String msg);
    }
}
