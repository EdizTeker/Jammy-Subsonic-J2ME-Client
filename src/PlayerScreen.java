import javax.microedition.lcdui.*;
import java.util.Vector;
import java.util.Timer;
import java.util.TimerTask;

public class PlayerScreen extends Form implements CommandListener, AudioPlayer.PlaybackListener {
    private MainMIDlet mainApp;
    private AudioPlayer audioPlayer;

    private Vector songList;
    private int currentIndex;

    private AlbumArtControl coverControl;
    private StringItem titleItem;
    private StringItem artistItem;
    private StringItem timeItem;

    private Command backCommand;
    private Command favCommand;
    private Command downloadSongCmd;
    private Command downloadAlbumCmd;
    private Command artistCommand;
    private Command albumCommand;
    private Command detailsCommand;

    private Command openControlsCmd;
    private Command closeControlsCmd;
    private List controlsMenu;

    private int currentVolume = 80;
    private Timer updateTimer;
    private int artSize;

    public PlayerScreen(MainMIDlet app, AudioPlayer player, Vector list, int index) {
        super("Now Playing");
        this.mainApp = app;
        this.audioPlayer = player;
        this.songList = list;
        this.currentIndex = index;

        Display d = Display.getDisplay(mainApp);
        this.artSize = 120;

        initUI();

        audioPlayer.setVolume(currentVolume);
        this.audioPlayer.setListener(this);

        loadSong(currentIndex);
    }

    public void onSongEnd() {
        Display.getDisplay(mainApp).callSerially(new Runnable() {
            public void run() {
                if (currentIndex < songList.size() - 1) {
                    currentIndex++;
                    loadSong(currentIndex);
                } else {
                    updateStatusText();
                }
            }
        });
    }

    public void onError(final String msg) {
        Display.getDisplay(mainApp).callSerially(new Runnable() {
            public void run() {
                Alert a = new Alert("Playback Error", msg, null, AlertType.ERROR);
                a.setTimeout(Alert.FOREVER);
                Display.getDisplay(mainApp).setCurrent(a);
            }
        });
    }

    private void initUI() {
        coverControl = new AlbumArtControl("", artSize);
        coverControl.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_AFTER);
        append(coverControl);

        titleItem = new StringItem(null, "Loading...\n");
        titleItem.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_AFTER);
        titleItem.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        append(titleItem);

        artistItem = new StringItem(null, "...\n");
        artistItem.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_AFTER);
        artistItem.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        append(artistItem);

        timeItem = new StringItem(null, "--:--   Vol: 80%");
        timeItem.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_AFTER);
        timeItem.setFont(Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        append(timeItem);

        backCommand = new Command("Back", Command.BACK, 0);
        downloadSongCmd = new Command("Download Song", Command.ITEM, 2);
        downloadAlbumCmd = new Command("Download Album", Command.ITEM, 6);
        artistCommand = new Command("Go to Artist", Command.ITEM, 3);
        albumCommand = new Command("Go to Album", Command.ITEM, 4);
        detailsCommand = new Command("Details", Command.ITEM, 5);

        addCommand(downloadSongCmd);
        addCommand(downloadAlbumCmd);
        addCommand(artistCommand);
        addCommand(albumCommand);
        addCommand(detailsCommand);
        addCommand(backCommand);

        openControlsCmd = new Command("Player Controls", Command.ITEM, 1);
        addCommand(openControlsCmd);

        String[] opts = { "Play/Pause", "Volume Up (+10)", "Volume Down (-10)", "Next Song", "Previous Song" };
        controlsMenu = new List("Controls", List.IMPLICIT, opts, null);

        closeControlsCmd = new Command("Back", Command.BACK, 0);
        controlsMenu.addCommand(closeControlsCmd);

        controlsMenu.setCommandListener(this);

        setCommandListener(this);
    }

    private void loadSong(int index) {
        if (index < 0 || index >= songList.size()) return;

        SongItem item = (SongItem) songList.elementAt(index);

        titleItem.setText(item.title);
        artistItem.setText(item.artist);
        coverControl.setImage(null);

        updateMenu(item);

        String streamUrl = mainApp.buildApiUrl("stream.view") + "&id=" + item.id;
        audioPlayer.play(streamUrl);

        final String cId = item.id;
        new Thread(new Runnable() {
            public void run() {
                if (cId != null && cId.length() > 0) {
                    String url = mainApp.buildApiUrl("getCoverArt.view") + "&id=" + cId + "&size=" + artSize;
                    final Image img = NetworkHelper.downloadImage(url, cId + "_" + artSize);
                    if (img != null) {
                        Display.getDisplay(mainApp).callSerially(new Runnable() {
                            public void run() { coverControl.setImage(img); }
                        });
                    }
                }
            }
        }).start();

        startTimer();
    }

    private void changeVolume(int delta) {
        currentVolume += delta;
        if (currentVolume > 100) currentVolume = 100;
        if (currentVolume < 0) currentVolume = 0;

        audioPlayer.setVolume(currentVolume);
        updateStatusText();
    }

    private void startTimer() {
        if (updateTimer != null) updateTimer.cancel();
        updateTimer = new Timer();
        updateTimer.schedule(new TimerTask() {
            public void run() { updateStatusText(); }
        }, 1000, 1000);
    }

    private void updateStatusText() {
        long currentMicro = audioPlayer.getMediaTime();
        long totalMicro = audioPlayer.getDuration();

        String currStr = formatTime(currentMicro);
        String totalStr = formatTime(totalMicro);

        final String vol = " Vol:" + currentVolume + "%";
        final String sym = audioPlayer.isPlaying ? " >" : " ||";
        final String status = currStr + " / " + totalStr + "  " + sym + vol;

        Display.getDisplay(mainApp).callSerially(new Runnable() {
            public void run() { timeItem.setText(status); }
        });
    }

    private String formatTime(long micro) {
        if (micro < 0) return "--:--";
        long totalSec = micro / 1000000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return min + ":" + (sec < 10 ? "0" + sec : "" + sec);
    }

    public void commandAction(Command c, Displayable d) {
        if (d == this) {

            if (c == openControlsCmd) {
                Display.getDisplay(mainApp).setCurrent(controlsMenu);
                return;
            }

            SongItem currentSong = (SongItem) songList.elementAt(currentIndex);

            if (c == backCommand) {
                if (updateTimer != null) updateTimer.cancel();
                audioPlayer.setListener(null);
                if ("fav".equals(currentSong.albumId)){
                    mainApp.showFavorites();
                } else {
                    mainApp.showSongList(currentSong.albumId);
                }
            }
            else if (c == favCommand) toggleFavorite(currentSong);
            else if (c == downloadSongCmd) {
                String fileName = currentSong.artist + " - " + currentSong.title + "." + Config.format;
                String url = mainApp.buildApiUrl("download.view") + "&id=" + currentSong.id;
                DownloadHelper.downloadFile(mainApp, url, fileName);
            }
            else if (c == downloadAlbumCmd) {
                if (currentSong.albumId != null) {
                    String fileName = currentSong.album + ".zip";
                    String url = mainApp.buildApiUrl("download.view") + "&id=" + currentSong.albumId;
                    DownloadHelper.downloadFile(mainApp, url, fileName);
                } else showAlert("Error", "Album ID not found");
            }
            else if (c == artistCommand) {
                if (currentSong.artistId != null) {
                    audioPlayer.setListener(null);
                    mainApp.showArtistAlbums(currentSong.artistId);
                } else showAlert("Error", "Artist ID missing");
            }
            else if (c == albumCommand) {
                if (currentSong.albumId != null) {
                    audioPlayer.setListener(null);
                    mainApp.showSongList(currentSong.albumId);
                } else showAlert("Error", "Album ID missing");
            }
            else if (c == detailsCommand) {
                String info = "Fmt: " + currentSong.format + "\nBit: " + currentSong.bitRate + "\nID: " + currentSong.id;
                showAlert("Details", info);
            }
        }

        else if (d == controlsMenu) {
            if (c == List.SELECT_COMMAND) {
                int idx = controlsMenu.getSelectedIndex();
                switch(idx) {
                    case 0:
                        if (audioPlayer.isPlaying) {
                            audioPlayer.pause();
                            controlsMenu.setTitle("Paused");
                        } else {
                            audioPlayer.resume();
                            controlsMenu.setTitle("Playing");
                        }
                        updateStatusText();
                        break;
                    case 1:
                        changeVolume(10);
                        controlsMenu.setTitle("Vol: " + currentVolume + "%");
                        break;
                    case 2:
                        changeVolume(-10);
                        controlsMenu.setTitle("Vol: " + currentVolume + "%");
                        break;
                    case 3:
                        if (currentIndex < songList.size() - 1) {
                            currentIndex++;
                            loadSong(currentIndex);
                            Display.getDisplay(mainApp).setCurrent(this);
                        }
                        break;
                    case 4:
                        if (currentIndex > 0) {
                            currentIndex--;
                            loadSong(currentIndex);
                            Display.getDisplay(mainApp).setCurrent(this);
                        }
                        break;
                }
            }
            else if (c == closeControlsCmd) {
                Display.getDisplay(mainApp).setCurrent(this);
            }
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(title, msg, null, AlertType.INFO);
        Display.getDisplay(mainApp).setCurrent(a);
    }

    private void toggleFavorite(final SongItem song) {
        song.isStarred = !song.isStarred;
        updateMenu(song);
        new Thread(new Runnable() {
            public void run() {
                String method = song.isStarred ? "star.view" : "unstar.view";
                String url = mainApp.buildApiUrl(method) + "&id=" + song.id;
                String response = NetworkHelper.performRequest(url);
                if (response == null || response.indexOf("error") != -1) {
                    song.isStarred = !song.isStarred;
                    Display.getDisplay(mainApp).callSerially(new Runnable() {
                        public void run() {
                            loadSong(currentIndex);
                            showAlert("Error", "Network Failed");
                        }
                    });
                }
            }
        }).start();
    }

    private void updateMenu(SongItem item) {
        if (favCommand != null) removeCommand(favCommand);
        if (item.isStarred) favCommand = new Command("Unfavorite", Command.ITEM, 1);
        else favCommand = new Command("Favorite", Command.ITEM, 1);
        addCommand(favCommand);
    }

    class AlbumArtControl extends CustomItem {
        private Image image;
        private int size;

        public AlbumArtControl(String label, int size) {
            super(label);
            this.size = size;
        }

        public void setImage(Image img) {
            this.image = img;
            repaint();
        }

        protected int getMinContentWidth() { return size; }
        protected int getMinContentHeight() { return size; }
        protected int getPrefContentWidth(int h) { return size; }
        protected int getPrefContentHeight(int w) { return size; }

        protected void paint(Graphics g, int w, int h) {
            if (image != null) {
                g.drawImage(image, w/2, h/2, Graphics.HCENTER | Graphics.VCENTER);
            } else {
                g.setColor(0x444444);
                g.fillRect((w-size)/2, (h-size)/2, size, size);
                g.setColor(0xFFFFFF);
                g.drawString("Loading...", w/2, h/2, Graphics.BASELINE | Graphics.HCENTER);
            }
        }

        protected void keyPressed(int keyCode) {
            int action = getGameAction(keyCode);
            if (action == Canvas.RIGHT) {
                if (currentIndex < songList.size() - 1) {
                    currentIndex++;
                    loadSong(currentIndex);
                }
            } else if (action == Canvas.LEFT) {
                if (currentIndex > 0) {
                    currentIndex--;
                    loadSong(currentIndex);
                }
            } else if (action == Canvas.UP) {
                changeVolume(10);
            } else if (action == Canvas.DOWN) {
                changeVolume(-10);
            } else if (action == Canvas.FIRE || keyCode == -5) {
                if (audioPlayer.isPlaying) audioPlayer.pause();
                else audioPlayer.resume();
                updateStatusText();
            }
        }
    }
}