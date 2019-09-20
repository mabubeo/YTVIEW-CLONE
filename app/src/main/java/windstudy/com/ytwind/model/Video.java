package windstudy.com.ytwind.model;

public class Video {
    private String link;
    private long firstTime;
    private long secondTime;
    private long playlistTime;
    private long fromTime;

    public Video(String link, long firstTime, long secondTime, long playlistTime, long fromTime) {
        this.link = link;
        this.firstTime = firstTime;
        this.secondTime = secondTime;
        this.playlistTime = playlistTime;
        this.fromTime = fromTime;
    }

    public Video() {
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public long getFirstTime() {
        return firstTime;
    }

    public void setFirstTime(long firstTime) {
        this.firstTime = firstTime;
    }

    public long getSecondTime() {
        return secondTime;
    }

    public void setSecondTime(long secondTime) {
        this.secondTime = secondTime;
    }

    public long getPlaylistTime() {
        return playlistTime;
    }

    public void setPlaylistTime(long playlistTime) {
        this.playlistTime = playlistTime;
    }

    public long getFromTime() {
        return fromTime;
    }

    public void setFromTime(long fromTime) {
        this.fromTime = fromTime;
    }
}
