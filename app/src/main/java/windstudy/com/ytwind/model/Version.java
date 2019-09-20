package windstudy.com.ytwind.model;

public class Version {
    private String link;
    private String version;

    public Version() {
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Version{" +
                "link='" + link + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
