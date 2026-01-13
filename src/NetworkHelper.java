import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Vector;


public class NetworkHelper {

    private static Hashtable imageCache = new Hashtable();
    private static Vector cacheKeys = new Vector();

    private static final String USER_AGENT = "J2ME-Subsonic-Client";

    public static Image downloadImage(String url, String key) {

        if (imageCache.containsKey(key)) {
            System.out.println("Cache Hit: " + key);
            return (Image) imageCache.get(key);
        }

        Image img = null;
        HttpConnection hc = null;
        InputStream is = null;
        try {
            hc = (HttpConnection) Connector.open(url);
            hc.setRequestMethod(HttpConnection.GET);
            hc.setRequestProperty("User-Agent", USER_AGENT);

            hc.setRequestProperty("Connection", "close");

            if (hc.getResponseCode() == HttpConnection.HTTP_OK) {
                is = hc.openInputStream();
                img = Image.createImage(is);

                if (img != null) {
                    addToCache(key, img);
                }
            }
        } catch (Exception e) {
            System.out.println("Download failed for: " + key);
        } finally {
            close(is, null, hc);
        }
        return img;
    }

    private static void addToCache(String key, Image img) {
        int maxCache = 50;
        try {
            maxCache = Integer.parseInt(Config.cacheSize);
        } catch (Exception e) {
            maxCache = 50;
        }

        if (cacheKeys.size() >= maxCache) {
            System.out.println("Cache Limit ("+maxCache+") reached. Cleaning old items...");

            int itemsToRemove = 10;
            for (int i = 0; i < itemsToRemove; i++) {
                if (cacheKeys.size() > 0) {
                    String oldKey = (String) cacheKeys.firstElement();
                    imageCache.remove(oldKey);
                    cacheKeys.removeElementAt(0);
                }
            }
            System.gc();
        }

        if (!imageCache.containsKey(key)) {
            imageCache.put(key, img);
            cacheKeys.addElement(key);
        }
    }

    public static String performRequest(String url) {
        HttpConnection hc = null;
        InputStream is = null;
        ByteArrayOutputStream bos = null;
        String response = null;

        try {
            hc = (HttpConnection) Connector.open(url);
            hc.setRequestMethod(HttpConnection.GET);
            hc.setRequestProperty("User-Agent", USER_AGENT);

            hc.setRequestProperty("Connection", "close");

            int code = hc.getResponseCode();
            if (code == HttpConnection.HTTP_OK) {
                is = hc.openInputStream();
                bos = new ByteArrayOutputStream();

                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }

                byte[] data = bos.toByteArray();

                int validLength = removeWhitespace(data);

                if (validLength > 0) {
                    response = new String(data, 0, validLength, "UTF-8");
                }
            } else {
                System.out.println("HTTP Error Code: " + code);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(is, bos, hc);
        }
        return response;
    }

    private static int removeWhitespace(byte[] data) {
        int w = 0; // Writes pointer.
        boolean inString = false;
        boolean escaped = false;

        for (int r = 0; r < data.length; r++) { // Reads pointer.
            byte b = data[r];
            char c = (char) b;

            // Handles the escaped characters.
            if (escaped) {
                escaped = false;
                data[w++] = b; // Writes the character literally.
                continue;
            }

            if (c == '\\') {
                escaped = true;
                data[w++] = b;
                continue;
            }

            // Toggles string mode on quotes.
            if (c == '"') {
                inString = !inString;
                data[w++] = b;
                continue;
            }

            // If inside a string keeps everything.
            if (inString) {
                data[w++] = b;
                continue;
            }

            // If outside a string, skips whitespaces.
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                continue;
            }

            // Keeps other characters.
            data[w++] = b;
        }

        return w;
    }

    public static String urlEncode(String s) {
        if (s == null) return "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                sb.append(c);
            } else {
                String hex = Integer.toHexString(c);
                sb.append('%');
                if (hex.length() < 2) sb.append('0');
                sb.append(hex);
            }
        }
        return sb.toString();
    }


    private static void close(InputStream is, OutputStream os, Connection c) {
        try {
            if (is != null) is.close();
            if (os != null) os.close();
            if (c != null) c.close();
        } catch (Exception e) {}
    }

    public static void clearCache() { //Don't forget to add this to settings.
        imageCache.clear();
        cacheKeys.removeAllElements();
        System.gc();
    }
}
