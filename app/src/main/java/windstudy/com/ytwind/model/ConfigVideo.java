package windstudy.com.ytwind.model;

public class ConfigVideo {
    private long firstTime;
    private long secondTime;
    private long playlistTime;
    private long fromTime;

    public ConfigVideo() {
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

