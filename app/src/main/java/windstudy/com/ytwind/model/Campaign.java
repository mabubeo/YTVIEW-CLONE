package windstudy.com.ytwind.model;

import android.util.Log;

import java.util.List;

public class Campaign {
    ConfigVideo config;
    long delay;
    List<Link> link;
    int total;
    int type;
    int status;
    String name;

    public Campaign() {
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConfigVideo getConfig() {
        return config;
    }

    public void setConfig(ConfigVideo config) {
        this.config = config;
    }

    public List<Link> getLink() {
        return link;
    }

    public void setLink(List<Link> link) {
        this.link = link;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        String result = "Campaign{" +
                "name " + name +
                ", delay=" + delay +
                ", total=" + total +
                ", type=" + type +
                ", campaign firsttime" + config.getFirstTime() +
                ", campaign secondtime" + config.getSecondTime() +
                ", campaign playlisttime" + config.getPlaylistTime() +
                ", campaign fromtime" + config.getFromTime() +
                '}';
        for (Link link : link) {
            result = result + " - " + link;
        }
        return result;
    }
}
