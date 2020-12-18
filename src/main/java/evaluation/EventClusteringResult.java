package evaluation;

public class EventClusteringResult {
    private double recallP, precisionP;
    private int clusters;
    private int events;
    private int eventsWithCluster;

    public EventClusteringResult(double recallP, double precisionP,
                                 int clusters, int events, int eventsWithCluster) {
        this.recallP = recallP;
        this.precisionP = precisionP;
        this.clusters = clusters;
        this.events = events;
        this.eventsWithCluster = eventsWithCluster;
    }


    public double getRecallP() {
        return recallP;
    }

    public void setRecallP(double recallP) {
        this.recallP = recallP;
    }

    public double getPrecisionP() {
        return precisionP;
    }

    public void setPrecisionP(double precisionP) {
        this.precisionP = precisionP;
    }

    public int getEventsWithCluster() {
        return eventsWithCluster;
    }

    public void setEventsWithCluster(int eventsWithCluster) {
        this.eventsWithCluster = eventsWithCluster;
    }

    public int getClusters() {
        return clusters;
    }

    public void setClusters(int clusters) {
        this.clusters = clusters;
    }

    public int getEvents() {
        return events;
    }

    public void setEvents(int events) {
        this.events = events;
    }

    public double getClusterEventRatio() {
        return (this.clusters * 1.0)/(1.0 * this.events);
    }

    public double getEventWithClusterRatio() {
        return (this.eventsWithCluster * 1.0)/(1.0 * this.events);
    }
}
