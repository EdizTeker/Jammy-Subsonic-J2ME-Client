import javax.microedition.lcdui.*;

public class SettingsForm extends Form implements CommandListener {
    private MainMIDlet mainApp;

    private TextField pathField;
    private TextField cacheField;
    private ChoiceGroup coverGroup;
    private ChoiceGroup bitrateGroup;
    private ChoiceGroup formatGroup;

    // Commands
    private Command saveCommand;
    private Command cancelCommand;

    // Data Arrays
    private String[] rateLabels = {"64 kbps", "128 kbps", "192 kbps", "320 kbps", "Original"};
    private String[] rateValues = {"64", "128", "192", "320", "0"};
    private String[] fmtLabels = {"MP3", "AAC", "WAV", "RAW"};
    private String[] fmtValues = {"mp3", "aac", "wav", "raw"};
    private String[] coverLabels = {"Show", "Hide"};

    public SettingsForm(MainMIDlet app) {
        super("Settings");
        this.mainApp = app;

        pathField = new TextField("Download Path:", Config.downloadPath, 100, TextField.ANY);

        cacheField = new TextField("Cache Size (Songs):", Config.cacheSize, 5, TextField.NUMERIC);

        coverGroup = new ChoiceGroup("Album Covers:", Choice.POPUP, coverLabels, null);
        coverGroup.setSelectedIndex(Config.loadAlbumArt.equals("1") ? 0 : 1, true);

        bitrateGroup = new ChoiceGroup("Audio Quality:", Choice.POPUP, rateLabels, null);
        int selectedRate = 1;
        for(int i=0; i<rateValues.length; i++) {
            if (rateValues[i].equals(Config.bitrate)) {
                selectedRate = i;
                break;
            }
        }
        bitrateGroup.setSelectedIndex(selectedRate, true);

        formatGroup = new ChoiceGroup("Audio Format:", Choice.POPUP, fmtLabels, null);
        int selectedFmt = 0;
        for(int i=0; i<fmtValues.length; i++) {
            if (fmtValues[i].equals(Config.format)) {
                selectedFmt = i;
                break;
            }
        }
        formatGroup.setSelectedIndex(selectedFmt, true);

        append(formatGroup);
        append(bitrateGroup);
        append(coverGroup);
        append(cacheField);
        append(pathField);
        append("Note: Path must start with file://");

        saveCommand = new Command("Save", Command.OK, 0);
        cancelCommand = new Command("Cancel", Command.BACK, 1);

        addCommand(saveCommand);
        addCommand(cancelCommand);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == cancelCommand) {
            mainApp.showMainMenu();
        } else if (c == saveCommand) {
            Config.downloadPath = pathField.getString();
            Config.cacheSize = cacheField.getString();

            Config.format = fmtValues[formatGroup.getSelectedIndex()];
            Config.bitrate = rateValues[bitrateGroup.getSelectedIndex()];

            Config.loadAlbumArt = (coverGroup.getSelectedIndex() == 0) ? "1" : "0";

            Config.save();

            Alert a = new Alert("Settings", "Configuration Saved!", null, AlertType.INFO);
            a.setTimeout(2000);
            Display.getDisplay(mainApp).setCurrent(a, mainApp.getMainMenu());
        }
    }
}

