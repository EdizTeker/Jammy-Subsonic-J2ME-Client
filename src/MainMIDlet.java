import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import java.util.Vector;

public class MainMIDlet extends MIDlet implements CommandListener {
    private Display display;
    private Form form;
    private Command exitCommand;
    private LoginScreen loginScreen;
    private MainMenu mainMenu;
    private ArtistList artistList;
    private AudioPlayer audioPlayer;
    private PlayerScreen currentScreen;
    private boolean isConnecting = false;
    private PlayerScreen playerScreen;

    public MainMIDlet() {}

    public void startApp() {
        Config.load();
        if (display == null) {
            display = Display.getDisplay(this);
            loginScreen = new LoginScreen(this);
        }
        if (audioPlayer == null) {
            audioPlayer = new AudioPlayer(this);
        }

        display.setCurrent(loginScreen);
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {}

    public void commandAction(Command c, Displayable d) {
        if (c == exitCommand) {
            destroyApp(false);
            notifyDestroyed();
        }
    }

    public void performLogin() {
        if (isConnecting) { // Makes sure login button isn't clicked more than once.
            return;
        }
        isConnecting = true;

        Alert loading = new Alert("", "Connecting to the server...", null, AlertType.INFO);
        loading.setTimeout(Alert.FOREVER);
        display.setCurrent(loading);

        // Does the network stuff in a thread.
        new Thread(new Runnable() {
            public void run() {
                try{

                    String apiCall = buildApiUrl("ping.view");

                    String response = NetworkHelper.performRequest(apiCall); // Gets server response.

                    if (response != null && response.indexOf("ok") != -1) {
                        if (mainMenu == null) {
                            mainMenu = new MainMenu(MainMIDlet.this);
                        }
                        display.setCurrent(mainMenu);
                    }
                    else {
                        Alert error = new Alert("Failed", "Login failed. Check URL/Pass.", null, AlertType.ERROR);
                        error.setTimeout(Alert.FOREVER);
                        display.setCurrent(error, loginScreen);
                    }

                }
                catch (Exception e) {
                    e.printStackTrace();
                    Alert error = new Alert("Error", e.getMessage(), null, AlertType.ERROR);
                    display.setCurrent(error, loginScreen);
                }
                finally {
                    isConnecting = false;
                }
            }
        }).start();
    }
    public String buildApiUrl(String method) {
        StringBuffer sb = new StringBuffer();
        sb.append(Config.serverUrl);
        if (!Config.serverUrl.endsWith("/")) sb.append("/");
        sb.append("rest/");
        sb.append(method);
        sb.append("?u=");
        sb.append(Config.username);
        sb.append("&p=");
        sb.append(Config.password);
        sb.append("&v=1.16.1&c=J2MEClient&f=json");

        if (method.equals("stream.view") || method.equals("download.view")) {

            sb.append("&format=");
            sb.append(Config.format);

            if (!Config.bitrate.equals("0")) {
                sb.append("&maxBitRate=");
                sb.append(Config.bitrate);

                sb.append("&estimateContentLength=true");
            }
        }

        return sb.toString();
    }

    public void showMainMenu() {
        if (mainMenu == null) {
            mainMenu = new MainMenu(this);
        }
        display.setCurrent(mainMenu);
    }
    public Displayable getMainMenu() {
        if (mainMenu == null) {
            showMainMenu();
        }
        return mainMenu;
    }

    public void showLogin() {
        display.setCurrent(loginScreen);
    }

    public void logout() {
        Config.password = "";
        Config.username = "";
        showLogin();
    }

    public void showAlbumPage(int offset) {
        AlbumList albumList = new AlbumList(this, offset);
        display.setCurrent(albumList);
    }

    public void showSearch() {
        SearchForm s = new SearchForm(this);
        display.setCurrent(s);
    }

    public void showArtistPage(int offset) {
        if (artistList == null) {
            artistList = new ArtistList(this, offset);
        } else {
            artistList.loadPage(offset);
        }
        display.setCurrent(artistList);
    }

    public void showArtistAlbums(String artistId) {
        AlbumList artistAlbumList = new AlbumList(this, artistId);
        display.setCurrent(artistAlbumList);
    }
    public void showSongList(String albumId) {
        SongList songList = new SongList(this, albumId);
        display.setCurrent(songList);
    }

    public void showSettings() {
        SettingsForm s = new SettingsForm(this);
        display.setCurrent(s);
    }

    public void showPlayer(Vector songList, int index) {
        if (currentScreen != null && currentScreen != playerScreen) {
            currentScreen = null;
            System.gc();
        }

        if (playerScreen == null) {
            playerScreen = new PlayerScreen(this, audioPlayer, songList, index);
        } else {
            playerScreen.updateData(songList, index);
        }

        currentScreen = playerScreen;
        display.setCurrent(playerScreen);
    }

    public void openNowPlaying() {
        if (currentScreen != null) {
            display.setCurrent(currentScreen);
        } else {
            Alert a = new Alert("", "Nothing is playing right now.", null, AlertType.INFO);
            display.setCurrent(a, mainMenu);
        }
    }

    public void showFavorites() {
        FavoritesList favs = new FavoritesList(this);
        display.setCurrent(favs);
    }


}
