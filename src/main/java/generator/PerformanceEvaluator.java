package generator;

import dbscan.GeoCluster;
import evaluation.ConfusionMatrix;
import evaluation.EventClusteringResult;
import model.*;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;
import util.GeoUtils;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.stream.Collectors.*;

public class PerformanceEvaluator {
    private final String outputPath;
    private final Collection<ShortTweet> tweets;
    private final Collection<Event> events;
    private final List<GeoCluster> clusters;
    private final Map<Event, Set<Point>> eventTweets;
    private final PrintWriter out;
    private ShapeLevel accuracyLevel;
    private EventClusteringResult averageResults;

    public PerformanceEvaluator(Collection<ShortTweet> tweets, Collection<Event> events, List<GeoCluster> clusters,
                                ShapeLevel accuracyLevel, String outputPath) throws FileNotFoundException, UnsupportedEncodingException {
        this.tweets = tweets;
        this.events = events;
        this.clusters = clusters;
        this.accuracyLevel = accuracyLevel;
        this.outputPath = outputPath;

        File file = new File(outputPath);
        file.getParentFile().mkdirs();
        out = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file), "UTF-8"));


        this.eventTweets =
                this.tweets.stream().filter(ShortTweet::isSubEventRelevant)
                        .collect(
                                groupingBy(ShortTweet::getEvent,
                                        mapping(t -> GeoUtils.getPoint(t.getHiddenLon(), t.getHiddenLat()), toSet())));
    }

    public EventClusteringResult getAverageResults() {
        return averageResults;
    }

    public void evaluate() {
        printInfo("\n***  STATISTICS ***");
        printInfo("Numero di eventi\t" + events.size());
        printInfo("Cluster trovati\t" + clusters.size());
        long relevant = this.tweets.stream().filter(t -> t.isRelevant()).count();
        long generic = this.tweets.stream().filter(t -> t.isGeneric()).count();
        long subevent = this.tweets.stream().filter(t -> t.isSubEventRelevant()).count();
        long subeventGeo = this.tweets.stream().filter(t -> t.isSubEventRelevant() && t.isGeo() && t.isPreciseGeo()).count();
        long subeventGeoEstimated = this.tweets.stream().filter(t -> t.isSubEventRelevant() && t.isGeo() && !t.isPreciseGeo()).count();

        printInfo("Tweet complessivi\t" + tweets.size());
        printInfo("Rilevanti\t" + relevant);
        printInfo("-Generici\t" + generic);
        printInfo("-Sottoeventi\t" + subevent);
        printInfo("--Geolocalized\t" + subeventGeo);
        printInfo("--Estimated\t" + subeventGeoEstimated);

        ConfusionMatrix cf;

        double sumPrecisionP = 0;
        double sumRecallP = 0;
        int eventsWithCluster = 0;

        for (Event e : events) {
            printInfo(e.getName());
            printInfo("Numero tweet sottevento: " + eventTweets.get(e).size());
            BaseShape tmp = e.getEntity();
            if (e.getEntity().getLevel().equals(ShapeLevel.POI)) {
                Poi poi = (Poi) e.getEntity();
                if (this.accuracyLevel.equals(ShapeLevel.POI))
                    tmp = poi;
                else if (this.accuracyLevel.equals(ShapeLevel.STREET))
                    tmp = poi.getStreet();
                else if (this.accuracyLevel.equals(ShapeLevel.DISTRICT))
                    tmp = poi.getDistrict();
                else if (this.accuracyLevel.equals(ShapeLevel.CITY))
                    tmp = poi.getCity();
            }
            final Shape shapeTP = tmp.getShape();

            // Escludo i punti che stanno al dì fuori AREA DI RIFERIMENTO
            Set<Point> tweetsCleaned = eventTweets.get(e).stream()
                    .filter(p -> GeoUtils.isContained(p, shapeTP)).collect(toSet());

            printInfo("Numero tweet dentro area " + tmp.getLevel() + ": " + tweetsCleaned.size());

            long TP = 0, FP = 0, TN = 0, FN = 0;
            long currentTP = 0;
            for (GeoCluster cluster : clusters) {
                Set<Point> clusterPoints = cluster.getPoints().stream().collect(toSet());
                Shape clusterShape = GeoUtils.convexHull(clusterPoints);

                // Punti che stanno dentro district e dentro il cluster
                currentTP = tweetsCleaned.stream().filter(t -> GeoUtils.isContained(t, clusterShape)).count();
                if (currentTP > TP) {
                    // Punti che stanno dentro AREA DI RIFERIMENTO e dentro il cluster
                    TP = currentTP;
                    // Punti dentro il cluster, ma fuori AREA DI RIFERIMENTO
                    FP = clusterPoints.stream().filter(p -> !GeoUtils.isContained(p, shapeTP)).count();
                    // Punti fuori dal cluster e fuori AREA DI RIFERIMENTO
                    TN = 0;
                    // Punti dentro AREA DI RIFERIMENTO, ma fuori dal cluster
                    FN = tweetsCleaned.stream().filter(t -> !GeoUtils.isContained(t, clusterShape)).count();
                }
            }

            if (TP > 0) {
                eventsWithCluster++;
            }

            cf = new ConfusionMatrix(TP, FP, TN, FN);
            sumPrecisionP += cf.getPrecisionP();
            sumRecallP += cf.getRecallP();
            printInfo(cf);

        }
        System.out.println("Eventi con cluster:\t" + eventsWithCluster);
        String results = "\nRESULT [Events With Cluster; Mean PrecisionP; Mean Recall]:\t" +
                (1.0 * eventsWithCluster) / (1.0 * events.size()) + "\t" +
                (sumPrecisionP / this.events.size()) + "\t" + (sumRecallP / this.events.size());
        printInfo(results);
        this.averageResults = new EventClusteringResult(
                (sumRecallP / this.events.size()),
                (sumPrecisionP / this.events.size()), clusters.size(), events.size(), eventsWithCluster);

        out.flush();
        out.close();


    }


    private void printInfo(Object s) {
        out.println(s);
        System.out.println(s);
    }


}
