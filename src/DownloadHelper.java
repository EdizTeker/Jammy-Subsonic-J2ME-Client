import javax.microedition.io.*;
import javax.microedition.io.file.*; // Requires JSR-75
import javax.microedition.lcdui.*;
import java.io.*;

public class DownloadHelper {

    public static void downloadFile(final MainMIDlet app, final String url, final String fileName) {

        final String fullPath = Config.downloadPath + sanitize(fileName);

        final String safeName = sanitize(fileName);

        Alert startAlert = new Alert("Download", "Downloading " + safeName + " to " + Config.downloadPath, null, AlertType.INFO);
        startAlert.setTimeout(2000);
        Display.getDisplay(app).setCurrent(startAlert);

        new Thread(new Runnable() {
            public void run() {
                FileConnection fconn = null;
                OutputStream out = null;
                HttpConnection http = null;
                InputStream in = null;

                try {
                    // Connects to the server.
                    http = (HttpConnection) Connector.open(url);
                    int code = http.getResponseCode();

                    // Checks for errors or redirects.
                    if (code != HttpConnection.HTTP_OK) {
                        throw new IOException("Server Error: " + code);
                    }

                    // Prepares file.
                    String filePath = fullPath + fileName;
                    fconn = (FileConnection) Connector.open(filePath, Connector.READ_WRITE);

                    // If file exists, deletes it.
                    if (fconn.exists()) {
                        fconn.delete();
                    }
                    fconn.create();

                    // The transfer loop.
                    in = http.openInputStream();
                    out = fconn.openOutputStream();

                    byte[] buffer = new byte[4096]; // 4KB buffer.
                    int len;
                    long totalRead = 0;

                    // Writes to the device.
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                        totalRead += len;
                    }

                    String sizeStr = (totalRead > 1024*1024) ?
                            (totalRead / (1024*1024)) + " MB" :
                            (totalRead / 1024) + " KB";

                    final String msg = "Saved: " + fileName + "\nSize: " + sizeStr;

                    Display.getDisplay(app).callSerially(new Runnable() {
                        public void run() {
                            Alert a = new Alert("Download Complete", msg, null, AlertType.ALARM);
                            a.setTimeout(Alert.FOREVER);
                            Display.getDisplay(app).setCurrent(a);
                        }
                    });

                } catch (Exception e) {
                    final String err = e.getMessage();
                    Display.getDisplay(app).callSerially(new Runnable() {
                        public void run() {
                            Alert a = new Alert("Download Failed", err, null, AlertType.ERROR);
                            Display.getDisplay(app).setCurrent(a);
                        }
                    });
                    e.printStackTrace();
                } finally {
                    // Cleanup.
                    try { if (in != null) in.close(); } catch (Exception e) {}
                    try { if (out != null) out.close(); } catch (Exception e) {}
                    try { if (http != null) http.close(); } catch (Exception e) {}
                    try { if (fconn != null) fconn.close(); } catch (Exception e) {}
                }
            }
        }).start();
    }

    private static String sanitize(String name) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            // Allows letters, numbers, spaces, dots, dashes, underscores.
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
                    (c >= '0' && c <= '9') || c == ' ' || c == '.' || c == '-' || c == '_') {
                sb.append(c);
            } else {
                // Replaces everything else (like / : ? *) with underscore.
                sb.append('_');
            }
        }
        return sb.toString();
    }
}