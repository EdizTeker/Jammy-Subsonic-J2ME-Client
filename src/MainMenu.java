import javax.microedition.lcdui.*;

public class MainMenu extends List implements CommandListener {
    private MainMIDlet mainApp;

    public MainMenu(MainMIDlet app) {
        super("Main Menu", List.IMPLICIT);
        this.mainApp = app;

        append("Now Playing", null);
        append("Favorites", null);
        append("Search", null);
        append("Albums ", null);
        append("Artists", null);
        append("Settings", null);
        append("Logout", null);

        addCommand(new Command("Exit", Command.EXIT, 0));
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c.getCommandType() == Command.EXIT) {
            mainApp.notifyDestroyed();
        } else if (c == List.SELECT_COMMAND) {
            int index = getSelectedIndex();
            switch(index) {
                case 0: mainApp.openNowPlaying(); break;
                case 1: mainApp.showFavorites(); break;
                case 2: mainApp.showSearch(); break;
                case 3: mainApp.showAlbumPage(0); break;
                case 4: mainApp.showArtistPage(0); break;
                case 5: mainApp.showSettings(); break;
                case 6: mainApp.logout(); break;
            }
        }
    }
}