import javax.microedition.lcdui.*;

public class LoginScreen extends Form implements CommandListener {
    private TextField urlField;
    private TextField userField;
    private TextField passField;
    private Command loginCommand;
    private Command exitCommand;

    private MainMIDlet mainApp;

    public LoginScreen(MainMIDlet app) {
        super("Login");
        this.mainApp = app;

        String initialUrl = (Config.serverUrl != null && Config.serverUrl.length() > 0) ? Config.serverUrl : "http://";
        String initialUser = (Config.username != null) ? Config.username : "";
        String initialPass = (Config.password != null) ? Config.password : "";

        urlField = new TextField("Server URL:", initialUrl, 255, TextField.URL);
        userField = new TextField("Username:", initialUser, 50, TextField.ANY);
        passField = new TextField("Password:", initialPass, 50, TextField.PASSWORD);

        append(urlField);
        append(userField);
        append(passField);

        loginCommand = new Command("Login", Command.OK, 1);
        exitCommand = new Command("Exit", Command.EXIT, 0);

        addCommand(loginCommand);
        addCommand(exitCommand);
        setCommandListener(this);

    }

    public void commandAction(Command c, Displayable d) {
        if (c == exitCommand) {
            mainApp.notifyDestroyed();
        } else if (c == loginCommand) {
            String url = urlField.getString();
            String user = userField.getString();
            String pass = passField.getString();

            if (url.length() < 7 || user.length() == 0 || pass.length() == 0) {
                Alert error = new Alert("Error", "Please fill all fields", null, AlertType.ERROR);
                Display.getDisplay(mainApp).setCurrent(error);
                return;
            }
            Config.serverUrl = url;
            Config.username = user;
            Config.password = pass;

            Config.save();


            mainApp.performLogin();
        }
    }
}
