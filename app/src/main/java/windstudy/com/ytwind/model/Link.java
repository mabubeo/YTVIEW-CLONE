package windstudy.com.ytwind.model;

public class Link {
    private String link;
    private long firstTime;
    private long secondTime;
    private long playlistTime;
    private long fromTime;
    private int count;

    public Link() {
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

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "Link{" +
                "link='" + link + '\'' +
                ", firstTime=" + firstTime +
                ", secondTime=" + secondTime +
                ", playlistTime=" + playlistTime +
                ", fromTime=" + fromTime +
                ", count=" + count +
                '}';
    }
}
