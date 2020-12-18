package model;

public class ShortTweet {
    String user, date, text;
    String location;
    double lat, lon;
    String classLabel;
    double hiddenLat, hiddenLon;
    boolean generic;
    boolean relevant;
    Event event;
    boolean inAreaEvent;


    public ShortTweet(String user, String date, String text) {
        this.user = user;
        this.date = date;
        this.text = text;
        this.generic = false;
        this.inAreaEvent = false;
    }

    public ShortTweet(String csv, String separator) {

        String[] data = csv.split(separator);
        this.user = data[0];
        this.date = data[1];
        this.text = data[2];
        this.location = data[3];
        this.lat = Double.parseDouble(data[4]);
        this.lon = Double.parseDouble(data[5]);
        this.classLabel = data[6];
        this.hiddenLat = Double.parseDouble(data[7]);
        this.hiddenLon = Double.parseDouble(data[8]);
        this.inAreaEvent = false;
    }

    public boolean isInAreaEvent() {
        return inAreaEvent;
    }

    public void setInAreaEvent(boolean inAreaEvent) {
        this.inAreaEvent = inAreaEvent;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public boolean isGeneric() {
        return generic;
    }

    public boolean isSubEventRelevant() {
        return relevant && !generic;
    }

    public boolean isGeo() {
        return lat >= 0 && lon >= 0;
    }

    public boolean isPreciseGeo() {
        return lat == hiddenLat && lon == hiddenLon;
    }

    public void setGeneric(boolean generic) {
        this.generic = generic;
    }

    public boolean isRelevant() {
        return relevant;
    }

    public void setRelevant(boolean relevant) {
        this.relevant = relevant;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getClassLabel() {
        return classLabel;
    }

    public void setClassLabel(String classLabel) {
        this.classLabel = classLabel;
    }

    public double getHiddenLat() {
        return hiddenLat;
    }

    public void setHiddenLat(double hiddenLat) {
        this.hiddenLat = hiddenLat;
    }

    public double getHiddenLon() {
        return hiddenLon;
    }

    public void setHiddenLon(double hiddenLon) {
        this.hiddenLon = hiddenLon;
    }

    @Override
    public String toString() {
        return user + ";" + date +
                ";" + text +
                ";" + location +
                ";" + lat +
                ";" + lon +
                ";" + classLabel +
                ";" + hiddenLat +
                ";" + hiddenLon +
                ";" + relevant +
                ";" + generic;
    }
}
