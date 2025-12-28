public class SongItem {
    public String id;
    public String title;
    public String artist;
    public String album;
    public String albumId;
    public String artistId;
    public boolean isStarred;
    public String format;
    public String size;
    public String bitRate;
    public String lyrics = null;

    public SongItem(String id, String t, String a, String c, String d, String artId, boolean star, String fmt, String sz, String br) {
        this.id = id; this.title = t; this.artist = a; this.album = c; this.albumId = d;
        this.artistId = artId;
        this.isStarred = star;
        this.format = fmt;
        this.size = sz;
        this.bitRate = br;
    }
}