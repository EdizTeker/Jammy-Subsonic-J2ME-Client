import javax.microedition.lcdui.*;

public class SearchForm extends Form implements CommandListener {
    private MainMIDlet mainApp;

    private TextField searchField;
    private ChoiceGroup typeGroup;

    private Command searchCommand;
    private Command backCommand;

    public SearchForm(MainMIDlet app) {
        super("Search");
        this.mainApp = app;

        searchField = new TextField("Query:", "", 50, TextField.ANY);
        append(searchField);

        String[] types = { "Songs", "Albums", "Artists" };
        typeGroup = new ChoiceGroup("Search Type:", Choice.EXCLUSIVE, types, null);
        typeGroup.setSelectedIndex(0, true);
        append(typeGroup);

        searchCommand = new Command("Search", Command.OK, 0);
        backCommand = new Command("Back", Command.BACK, 1);

        addCommand(searchCommand);
        addCommand(backCommand);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            mainApp.showMainMenu();
        } else if (c == searchCommand) {
            String query = searchField.getString();

            if (query == null || query.length() == 0) {
                Alert a = new Alert("", "Please enter text", null, AlertType.WARNING);
                Display.getDisplay(mainApp).setCurrent(a);
                return;
            }

            int type = typeGroup.getSelectedIndex();

            SearchResultList results = new SearchResultList(mainApp, query, type);
            Display.getDisplay(mainApp).setCurrent(results);
        }
    }
}