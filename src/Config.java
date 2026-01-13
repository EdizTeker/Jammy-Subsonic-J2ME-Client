import javax.microedition.rms.*;
import java.io.*;

public class Config {
    public static String serverUrl = "";
    public static String username = "";
    public static String password = "";
    public static String downloadPath = "file:///c:/Data/Sounds/";
    public static String bitrate = "128";
    public static String format = "mp3";

    public static String cacheSize = "50";
    public static String loadAlbumArt = "1";

    private static final String RECORD_STORE_NAME = "SubsonicConfig";

    public static void load() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(RECORD_STORE_NAME, true);
            if (rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                DataInputStream dis = new DataInputStream(bais);

                serverUrl = dis.readUTF();
                username = dis.readUTF();
                password = dis.readUTF();
                downloadPath = dis.readUTF();
                bitrate = dis.readUTF();
                format = dis.readUTF();

                if (dis.available() > 0) {
                    cacheSize = dis.readUTF();
                    loadAlbumArt = dis.readUTF();
                }

                dis.close();
            }
        } catch (Exception e) {
            System.out.println("Config load error or first run.");
        } finally {
            try { if (rs != null) rs.closeRecordStore(); } catch (Exception e) {}
        }
    }

    public static void save() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(RECORD_STORE_NAME, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeUTF(serverUrl);
            dos.writeUTF(username);
            dos.writeUTF(password);
            dos.writeUTF(downloadPath);
            dos.writeUTF(bitrate);
            dos.writeUTF(format);

            dos.writeUTF(cacheSize);
            dos.writeUTF(loadAlbumArt);

            byte[] data = baos.toByteArray();

            if (rs.getNumRecords() == 0) {
                rs.addRecord(data, 0, data.length); // Create new.
            } else {
                rs.setRecord(1, data, 0, data.length); // Update existing.
            }

            dos.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.closeRecordStore(); } catch (Exception e) {}
        }
    }
}
