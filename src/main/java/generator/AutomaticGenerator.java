package generator;

import dbscan.ElkiDBSCAN;
import evaluation.EventClusteringResult;
import model.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;
import util.GeoUtils;
import util.KMLUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AutomaticGenerator {

    private static Shape noShape;
    private static Shape noShape2;

    enum TWEET_CLASS {YES, NO}

    private static String configurationName;
    private static String outputPath;
    private static Random randomEvents;
    private static Random randomTweets;
    private static Random randomSeeds;
    private static List<String> sentenceYes;
    private static List<ShortTweet> tweetNo;
    private static List<Event> events;
    private static List<ShortTweet> generatedTweets;
    private static Map<String, Street> streets;
    private static Map<String, District> districts;
    private static Map<String, City> cities;
    private static Map<String, Poi> pois;

    // Paramentri di configurazione
    private static String separator;
    private static String automaticCity;
    private static ResourceBundle generatorProperties;
    private static int numMinRelevantTweetPerEvent;
    private static int numMaxRelevantTweetPerEvent;
    private static double percRelevant;
    private static double percGeotagged;
    private static double percGenericRelevant;
    private static boolean isLatLon;
    private static double poiMinRadius;
    private static double poiMaxRadius;
    private static double streetRadiusRatio;
    private static double districtRadiusRatio;
    private static double percGeoInfoInTextPoi;
    private static double percGeoInfoInTextStreet;
    private static double percGeoInfoInTextDistrict;
    private static double percGeoInfoInTextCity;
    private static int numEvents;
    private static int numSeeds;
    private static long randomSeedEvents;
    private static long randomSeedTweets;
    private static long randomSeedSeeds;
    private static double percRelevantInPoI;
    private static double percRelevantInStreet;
    private static double percRelevantInDistrict;
    private static double percRelevantInCity;
    private static ShapeLevel evaluationAccuracyLevel;
    private static List<Event> eventSamples;
    private static List<EventClusteringResult> results;
    private static List<EventClusteringResult> resultsPreciseGeo;


    private static int minNumRelevantTweetsGeneratedPerEvent = Integer.MAX_VALUE;

    public static void main(String[] args) throws IOException {

        System.out.println(">>> START GENERATOR <<<");

        Scanner in = new Scanner(System.in);
        System.out.println("Enter configuration ID: ");
        int confId = in.nextInt();
        System.out.println("Set events: ");
        int events = in.nextInt();
        // Carica configurazione generatore
        generatorProperties = ResourceBundle.getBundle("config.generator" + confId);
        initGenerator();
        if (events > 0) {
            numEvents = events;
        }

        for (int i = 0; i < numSeeds; i++) {
            randomSeedTweets = randomSeeds.nextLong();
            randomSeedEvents = randomSeeds.nextLong();
            randomTweets = new Random(randomSeedTweets);
            randomEvents = new Random(randomSeedEvents);

            launchGenerator();

        }

        double sumRecall = 0.0, sumPrecision = 0.0, sumEventWithClusterRatio = 0.0;
        for (EventClusteringResult res : results) {
            sumRecall += res.getRecallP();
            sumPrecision += res.getPrecisionP();
            sumEventWithClusterRatio += res.getEventWithClusterRatio();
        }

        String resOutput = "\nFINALRESULT [EventsWithCluster; Mean PrecisionP; Mean Recall]:\n" +
                sumEventWithClusterRatio / numSeeds + "\t" +
                sumPrecision / numSeeds + "\t" + sumRecall / numSeeds;

        System.out.println("\n" + resOutput);

        sumRecall = 0.0;
        sumPrecision = 0.0;
        sumEventWithClusterRatio = 0.0;
        for (EventClusteringResult res : resultsPreciseGeo) {
            sumRecall += res.getRecallP();
            sumPrecision += res.getPrecisionP();
            sumEventWithClusterRatio += res.getEventWithClusterRatio();
        }

        resOutput = "\nFINALRESULT PRECISE GEO [EventsWithCluster; Mean PrecisionP; Mean Recall]:\n" +
                sumEventWithClusterRatio / numSeeds + "\t" +
                sumPrecision / numSeeds + "\t" + sumRecall / numSeeds;

        System.out.println("\n" + resOutput);


    }

    private static void launchGenerator() throws IOException {

        events = new LinkedList<>();
        generatedTweets = new LinkedList<>();
        streets = new HashMap<>();
        districts = new HashMap<>();
        pois = new HashMap();

        // Genero Tweets per ciascun evento
        generateEventTweets();
        generateEventKMLs(events);

        List<String> tweetsText = generatedTweets.stream().map(t -> t.toString()).collect(Collectors.toList());
        tweetsText.add(0, "USER;DATE;TEXT;LOCATION;LAT;LON;CLASS;HIDDEN_LAT;HIDDEN_LON;RELEVANT;GENERIC");
        Files.write(Paths.get(outputPath + "generatedDataset.csv"), tweetsText, StandardCharsets.UTF_8);

        List<String> tweetsKML = generatedTweets.stream().map(t -> tweetToKML(t, true)).filter(Objects::nonNull).collect(Collectors.toList());
        storeInFile(tweetsKML, "generatedDataset.kml");

        List<String> tweetsRelevantKML = generatedTweets.stream().filter(t -> t.isRelevant())
                .map(t -> tweetToKML(t, true)).collect(Collectors.toList());
        storeInFile(tweetsRelevantKML, "generatedRelevantDataset.kml");

        List<String> tweetsSubEventRelevantKML = generatedTweets.stream().filter(t -> t.isSubEventRelevant())
                .map(t -> tweetToKML(t, true)).collect(Collectors.toList());
        storeInFile(tweetsSubEventRelevantKML, "generatedSubEventRelevantDataset.kml");

        List<String> tweetsGenericKML = generatedTweets.stream().filter(t -> t.isGeneric())
                .map(t -> tweetToKML(t, true)).collect(Collectors.toList());
        storeInFile(tweetsGenericKML, "generatedGenericDataset.kml");

        List<String> tweetsNotRelevantKML = generatedTweets.stream().filter(t -> !t.isRelevant())
                .map(t -> tweetToKML(t, true)).collect(Collectors.toList());
        storeInFile(tweetsNotRelevantKML, "generatedNotRelevantDataset.kml");

        // Preparo dataset per DBSCAN
        long dbscanGeoEstimated = generatedTweets.stream().filter(t -> t.isSubEventRelevant() && t.isGeo() && !t.isPreciseGeo()).count();
        long dbscanGeoPrecise = generatedTweets.stream().filter(t -> t.isSubEventRelevant() && t.isPreciseGeo()).count();

        List<String> pointsDBSCAN = generatedTweets.stream().filter(t -> t.isSubEventRelevant() && t.isGeo())
                .map(x -> extractCoordinates(x)).collect(Collectors.toList());

        Files.write(Paths.get(outputPath + "dbscanDB.csv"), pointsDBSCAN, StandardCharsets.UTF_8);

        List<String> pointsDBSCANCoordinatesKML = pointsDBSCAN.stream()
                .map(x -> {
                    try {
                        return KMLUtils.serialize(GeoUtils.getPoint(x, separator, isLatLon));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());
        storeInFile(pointsDBSCANCoordinatesKML, "dbscanDB.kml");

        System.out.println("\n*** KML DATA ***");
        System.out.println("Tweet [total]: " + generatedTweets.size());
        System.out.println("Relevant: " + tweetsRelevantKML.size());
        System.out.println("\t->Generic: " + tweetsGenericKML.size());
        System.out.println("\t->SubEvents: " + tweetsSubEventRelevantKML.size());

        System.out.println("\t\t->Geolocalized: " + dbscanGeoPrecise);
        System.out.println("\t\t->Estimated: " + dbscanGeoEstimated);

        System.out.println("Not Relevant: " + tweetsNotRelevantKML.size());
        System.out.println("DBSCAN Points: " + pointsDBSCANCoordinatesKML.size());

        if (Boolean.parseBoolean(generatorProperties.getString("launchDBSCAN"))) {
            double DBSCAN_EPSILON = Double.parseDouble(generatorProperties.getString("eps"));
            int DBSCAN_MIN_PTS = Integer.parseInt(generatorProperties.getString("minPts"));

            if (DBSCAN_MIN_PTS > minNumRelevantTweetsGeneratedPerEvent) {
                DBSCAN_MIN_PTS = minNumRelevantTweetsGeneratedPerEvent / 2;
                System.err.println("The MinPts value is too low! It is set to: " + DBSCAN_MIN_PTS);
            }

            System.out.println("\n*** STARTING DBSCAN ***");
            System.out.println("Eps: " + DBSCAN_EPSILON);
            System.out.println("MinPts: " + DBSCAN_MIN_PTS);

            ElkiDBSCAN dbscan = new ElkiDBSCAN(pointsDBSCAN, DBSCAN_EPSILON, DBSCAN_MIN_PTS, separator, isLatLon);
            dbscan.cluster();
            dbscan.clustersToKML("DBSCAN_clusters.kml", false);

            // Calcola Precision e recall
            PerformanceEvaluator evaluator = new PerformanceEvaluator(
                    generatedTweets, events, dbscan.getAllGeoClusters(false),
                    evaluationAccuracyLevel, generateEvaluationFileName());
            evaluator.evaluate();

            results.add(evaluator.getAverageResults());

            System.out.println("\n*** STARTING DBSCAN WITHOUT ESTIMATED GEOTAGGED ***");
            System.out.println("Eps: " + DBSCAN_EPSILON);
            System.out.println("MinPts: " + DBSCAN_MIN_PTS);
            List<String> pointsPreciseGeoDBSCAN = generatedTweets.stream().filter(t -> t.isSubEventRelevant() && t.isPreciseGeo())
                    .map(x -> extractCoordinates(x)).collect(Collectors.toList());
            dbscan = new ElkiDBSCAN(pointsPreciseGeoDBSCAN, DBSCAN_EPSILON, DBSCAN_MIN_PTS, separator, isLatLon);
            dbscan.cluster();
            dbscan.clustersToKML("DBSCAN_clusters_precise.kml", false);

            // Calcola Precision e recall
            evaluator = new PerformanceEvaluator(
                    generatedTweets, events, dbscan.getAllGeoClusters(false),
                    evaluationAccuracyLevel, generateEvaluationFileNamePreciseGeo());
            evaluator.evaluate();

            resultsPreciseGeo.add(evaluator.getAverageResults());

        }

    }

    private static void generateEventTweets() {
        City city = cities.get(automaticCity);

//        @poi1;@street1;CIRCLE:16.4238:41.2785:100
//        @poi2;@street2;CIRCLE:16.42576:41.27554:100
//        @poi3;@street3;CIRCLE:16.41987,41.27493:100
//        @poi4;@street4;CIRCLE:16.41795:41.27833:100

//        @poi1;@street1;CIRCLE:16.4238:41.2785:100
//        @poi2;@street2;CIRCLE:16.42576:41.27554:100
//        @poi3;@street3;CIRCLE:16.41987,41.27493:100
//        @poi4;@street4;CIRCLE:16.41795:41.27833:100



        Poi p1 = generateFixedPoi(city,
                GeoUtils.getPoint(16.4238, 41.2785),
                "@poix1", "@streetx1", "@districtx1", 30);
        Poi p2 = generateFixedPoi(city,
                GeoUtils.getPoint(16.42576, 41.27554),
                "@poix2", "@streetx2", "@districtx2", 30);

        Poi p3 = generateFixedPoi(city,
                GeoUtils.getPoint(16.41987, 41.27493),
                "@poix3", "@streetx3", "@districtx3", 35);

        Poi p4 = generateFixedPoi(city,
                GeoUtils.getPoint(16.41795, 41.27833),
                "@poix4", "@streetx4", "@districtx4", 45);

        Poi p5 = generateFixedPoi(city,
                GeoUtils.getPoint(16.42718,41.26842),
                "@poix5", "@streetx5", "@districtx5", 150);

        Poi p6 = generateFixedPoi(city,
                GeoUtils.getPoint(        16.392599106,41.264122009),
                "@poix6", "@streetx6", "@districtx6", 120);


        Poi p7 = generateFixedPoi(city,
                GeoUtils.getPoint(   16.395479202,41.281257629),
                "@poix7", "@streetx7", "@districtx7", 135);

        Poi p8 = generateFixedPoi(city,
                GeoUtils.getPoint( 16.411159515,41.256710052),
                "@poix8", "@streetx8", "@districtx8", 90);
        Poi p9 = generateFixedPoi(city,
                GeoUtils.getPoint( 16.415361404,41.257640839),
                "@poix9", "@streetx9", "@districtx9", 102);
        Poi p10 = generateFixedPoi(city,
                GeoUtils.getPoint(16.400543213, 41.271083832),
                "@poix10", "@streetx10", "@districtx10", 90);

        Poi p11 = generateFixedPoi(city,
                GeoUtils.getPoint(16.406002045,41.266651154),
                "@poix11", "@streetx11", "@districtx11", 140);
        Poi p12 = generateFixedPoi(city,
                GeoUtils.getPoint(16.409269333,41.275005341),
                "@poix12", "@streetx12", "@districtx12", 96);

        Collection<Coordinate> coords = new LinkedList<>();
        coords.add(new Coordinate(16.3846552541001, 41.2934451884841));
        coords.add(new Coordinate(16.4491909704496, 41.2678161719434));
        coords.add(new Coordinate(16.448985114895, 41.2934451884841));
        GeometryFactory geometryFactory = new GeometryFactory();
        MultiPoint mp = geometryFactory.createMultiPointFromCoords(coords.toArray(new Coordinate[0]));
        Geometry geoHull = mp.convexHull();
        noShape = new JtsGeometry(geoHull, JtsSpatialContext.GEO, false, false);
        coords.add(new Coordinate(16.4135665382545, 41.2756437760935));
        coords.add(new Coordinate(16.4284588406566, 41.2698503505767));
        coords.add(new Coordinate(16.4254021867029, 41.2847071104909));
        mp = geometryFactory.createMultiPointFromCoords(coords.toArray(new Coordinate[0]));
        geoHull = mp.convexHull();
        noShape2 = new JtsGeometry(geoHull, JtsSpatialContext.GEO, false, false);

        List<Poi> manualPoi = new LinkedList<>();
        manualPoi.add(p1);
        manualPoi.add(p2);
        manualPoi.add(p3);
        manualPoi.add(p4);
        manualPoi.add(p5);
        manualPoi.add(p6);
        manualPoi.add(p7);
        manualPoi.add(p8);
        manualPoi.add(p9);
        manualPoi.add(p10);
        manualPoi.add(p11);
        manualPoi.add(p12);

        for (Poi poi : manualPoi) {
            Event eventSample = eventSamples.get(randomEvents.nextInt(eventSamples.size()));
            Event e = new Event("@event" + events.size(), eventSample.getType(), poi);
            e.setDescriptions(eventSample.getDescriptions());
            events.add(e);
            generateTweets(e);
        }

//        for (int i = 0; i < numEvents; i++) {
//            Poi poi = generateRandomPoi(city);
//            Event eventSample = eventSamples.get(randomEvents.nextInt(eventSamples.size()));
//            Event e = new Event("@event" + events.size(), eventSample.getType(), poi);
//            e.setDescriptions(eventSample.getDescriptions());
//            events.add(e);
//            generateTweets(e);
//        }
    }

    private static String generateEvaluationFileNamePreciseGeo() {
        return outputPath + "/" + configurationName + "/RES_EVENTS_" + numEvents + "_SEEDE_" + randomSeedEvents + "_SEEDT_" + randomSeedTweets
                + "-prec.txt";
    }

    private static String generateEvaluationFileName() {
        return outputPath + "/" + configurationName + "/RES_EVENTS_" + numEvents + "_SEEDE_" + randomSeedEvents + "_SEEDT_" + randomSeedTweets
                + ".txt";
    }

    private static void initGenerator() throws IOException {

        // Carico parametri di configurazione
        outputPath = generatorProperties.getString("outputPath");
        automaticCity = generatorProperties.getString("automaticCity");
        configurationName = generatorProperties.getString("configurationName");
        separator = generatorProperties.getString("csvSeparator");
        numMinRelevantTweetPerEvent = Integer.parseInt(generatorProperties.getString("numMinRelevantTweetPerEvent"));
        numMaxRelevantTweetPerEvent = Integer.parseInt(generatorProperties.getString("numMaxRelevantTweetPerEvent"));
        percGeotagged = Double.parseDouble(generatorProperties.getString("percGeotagged"));
        percGenericRelevant = Double.parseDouble(generatorProperties.getString("percGenericRelevant"));
        percRelevant = Double.parseDouble(generatorProperties.getString("percRelevant"));
        isLatLon = Boolean.parseBoolean(generatorProperties.getString("LatLon"));
        numEvents = Integer.parseInt(generatorProperties.getString("numEvents"));
        numSeeds = Integer.parseInt(generatorProperties.getString("numSeeds"));
        randomSeedSeeds = Long.parseLong(generatorProperties.getString("randomSeedSeeds"));
        randomSeeds = new Random(randomSeedSeeds);

        String accuracyLevel = generatorProperties.getString("evaluationAccuracyLevel");
        for (ShapeLevel level : ShapeLevel.values()) {
            if (accuracyLevel.equalsIgnoreCase(level.name())) {
                evaluationAccuracyLevel = level;
                break;
            }
        }

        poiMinRadius = Double.parseDouble(generatorProperties.getString("poiMinRadius"));
        poiMaxRadius = Double.parseDouble(generatorProperties.getString("poiMaxRadius"));
        streetRadiusRatio = Double.parseDouble(generatorProperties.getString("streetRadiusRatio"));
        districtRadiusRatio = Double.parseDouble(generatorProperties.getString("districtRadiusRatio"));

        percGeoInfoInTextPoi = Double.parseDouble(generatorProperties.getString("percGeoInfoInTextPoi"));
        percGeoInfoInTextStreet = Double.parseDouble(generatorProperties.getString("percGeoInfoInTextStreet"));
        percGeoInfoInTextDistrict = Double.parseDouble(generatorProperties.getString("percGeoInfoInTextDistrict"));
        percGeoInfoInTextCity = Double.parseDouble(generatorProperties.getString("percGeoInfoInTextCity"));

        percRelevantInPoI = Double.parseDouble(generatorProperties.getString("percRelevantInPoI"));
        percRelevantInStreet = Double.parseDouble(generatorProperties.getString("percRelevantInStreet"));
        percRelevantInDistrict = Double.parseDouble(generatorProperties.getString("percRelevantInDistrict"));
        percRelevantInCity = Double.parseDouble(generatorProperties.getString("percRelevantInCity"));

        if (Math.abs(percRelevantInPoI + percRelevantInStreet + percRelevantInDistrict + percRelevantInCity - 1.0) > 0.001) {
            throw new IllegalArgumentException("Disitrbution of percRelevant among areas must be equal to 1");
        }

        // Carico file CSV per generazione eventi
        sentenceYes = Files.lines(Paths.get("src/main/resources/sentenceYes.csv")).skip(1)
                .filter(Objects::nonNull).collect(Collectors.toList());
        tweetNo = Files.lines(Paths.get("src/main/resources/tweetNo.csv")).skip(1)
                .map(AutomaticGenerator::csvToTweet).filter(Objects::nonNull).collect(Collectors.toList());

        cities = Files.lines(Paths.get("src/main/resources/cities.csv")).skip(1)
                .map(AutomaticGenerator::csvToCity).filter(Objects::nonNull)
                .collect(Collectors.toMap(City::getName, Function.identity()));

//        generatorProperties.keySet().stream().forEach(e -> System.out.println(e + ": " + generatorProperties.getString(e)));

        // Carico esempi di eventi
        eventSamples = Files.lines(Paths.get("src/main/resources/eventSamples.csv"))
                .skip(1).map(AutomaticGenerator::csvToEvent).filter(Objects::nonNull).collect(Collectors.toList());

        results = new LinkedList<>();
        resultsPreciseGeo = new LinkedList<>();

    }

    private static Poi generateFixedPoi(City city, Point p, String poiName,
                                        String streetName, String districtName, double radiusPoi) {
        double radiusStreet = streetRadiusRatio * radiusPoi;
        double radiusDistrict = districtRadiusRatio * radiusPoi;
        District district = new District(districtName, GeoUtils.getCircle(p, radiusDistrict), city);
        Street street = new Street(streetName, GeoUtils.getCircle(p, radiusStreet), district);
        Poi poi = new Poi(poiName, GeoUtils.getCircle(p, radiusPoi), street);
        pois.put(poiName, poi);
        streets.put(streetName, street);
        districts.put(districtName, district);
        return poi;

    }

    private static Poi generateRandomPoi(City city) {

        boolean found = false;
        Point p;
        do {
            p = GeoUtils.getRandomPointInsideShape(city.getShape(), randomTweets);
            if (!GeoUtils.isContained(p, noShape) && !GeoUtils.isContained(p, noShape2))
                found = true;
            else
                System.out.println("No--" + p.toString());
        } while (!found);
        String poiName = "@poi" + pois.size();
        String streetName = "@street" + streets.size();
        String districtName = "@district" + districts.size();
        double radiusPoi = poiMinRadius + randomEvents.nextDouble() * (poiMaxRadius - poiMinRadius);
        double radiusStreet = streetRadiusRatio * radiusPoi;
        double radiusDistrict = districtRadiusRatio * radiusPoi;
        District district = new District(districtName, GeoUtils.getCircle(p, radiusDistrict), city);
        Street street = new Street(streetName, GeoUtils.getCircle(p, radiusStreet), district);
        Poi poi = new Poi(poiName, GeoUtils.getCircle(p, radiusPoi), street);
        pois.put(poiName, poi);
        streets.put(streetName, street);
        districts.put(districtName, district);
        return poi;

    }

    private static Object extractRandomElementFromMap(Map map, Random random) {
        List keys = new ArrayList(map.keySet());
        Object randomKey = keys.get(random.nextInt(keys.size()));
        return map.get(randomKey);
    }

    private static void storeInFile(List<String> data, String s) throws IOException {
        data.add(0, KMLUtils.OPEN_TAGS);
        data.add(KMLUtils.CLOSE_TAGS);
        Files.write(Paths.get(generatorProperties.getString("outputPath") + s), data, StandardCharsets.UTF_8);
        data.remove(0);
        data.remove(data.size() - 1);
    }

    private static String extractCoordinates(ShortTweet x) {
        if (x.isGeo()) {
            if (isLatLon) {
                return x.getLat() + separator + x.getLon();
            } else {
                return x.getLon() + separator + x.getLat();
            }
        } else
            return null;
    }

    private static void generateEventKMLs(List<Event> events) throws IOException {
        Set<Shape> currentPois = new HashSet<>();
        Set<Shape> currentStreets = new HashSet<>();
        Set<Shape> currentDistricts = new HashSet<>();
        Set<Shape> currentCities = new HashSet<>();

        events.forEach(e -> {
            if (e.getLevel() == ShapeLevel.POI) {
                Poi poi = (Poi) e.getEntity();
                currentPois.add(poi.getShape());
                currentStreets.add(poi.getStreet().getShape());
                currentDistricts.add(poi.getDistrict().getShape());
                currentCities.add(poi.getCity().getShape());
            } else if (e.getLevel() == ShapeLevel.STREET) {
                Street street = (Street) e.getEntity();
                currentStreets.add(street.getShape());
                currentDistricts.add(street.getDistrict().getShape());
                currentCities.add(street.getCity().getShape());
            } else if (e.getLevel() == ShapeLevel.DISTRICT) {
                District district = (District) e.getEntity();
                currentDistricts.add(district.getShape());
                currentCities.add(district.getCity().getShape());
            } else if (e.getLevel() == ShapeLevel.CITY) {
                currentCities.add(e.getEntity().getShape());
            }
        });

        List<String> poisKML = new LinkedList<>();
        List<String> streetsKML = new LinkedList<>();
        List<String> districtsKML = new LinkedList<>();
        List<String> citiesKML = new LinkedList<>();

        poisKML.addAll(generateEntityKMLs(currentPois));
        streetsKML.addAll(generateEntityKMLs(currentStreets));
        districtsKML.addAll(generateEntityKMLs(currentDistricts));
        citiesKML.addAll(generateEntityKMLs(currentCities));

        Files.write(Paths.get(outputPath + "generatedPois.kml"), poisKML, StandardCharsets.UTF_8);
        Files.write(Paths.get(outputPath + "generatedStreets.kml"), streetsKML, StandardCharsets.UTF_8);
        Files.write(Paths.get(outputPath + "generatedDistricts.kml"), districtsKML, StandardCharsets.UTF_8);
        Files.write(Paths.get(outputPath + "generatedCities.kml"), citiesKML, StandardCharsets.UTF_8);

    }

    private static List<String> generateEntityKMLs(Collection<Shape> shapes) {
        List<String> kmls = shapes.stream().map(e -> {
            try {
                return KMLUtils.serialize(e, false, null);
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
        kmls.add(0, KMLUtils.OPEN_TAGS);
        kmls.add(KMLUtils.CLOSE_TAGS);
        return kmls;
    }

    private static String tweetToKML(ShortTweet t, boolean geoHidden) {
        Point p = null;
        if (geoHidden)
            p = GeoUtils.getPoint(t.getHiddenLon(), t.getHiddenLat());
        else
            p = GeoUtils.getPoint(t.getLon(), t.getLat());
        Map<String, String> ext = new Hashtable<>();
        try {
            return KMLUtils.serialize(p, false, ext);
        } catch (IOException e) {
            System.err.println("Skipped with errors! " + t);
            return null;
        }
    }

    private static void generateTweets(Event e) {

        // Numero di tweet rilevant e non rilevanti
        int numRelevantTotal = numMinRelevantTweetPerEvent + randomTweets.nextInt(numMaxRelevantTweetPerEvent - numMinRelevantTweetPerEvent);
        int numTotal = (int) (numRelevantTotal / percRelevant);
        int numNotRelevantTotal = numTotal - numRelevantTotal;

        // Parametro per fare un auto-tuning del minPts di DBSCAN
        if (numRelevantTotal < minNumRelevantTweetsGeneratedPerEvent) {
            minNumRelevantTweetsGeneratedPerEvent = numRelevantTotal;
        }


        // Calcolo rilevanti per ogni area
        int numRelevantInPoI = (int) (percRelevantInPoI * numRelevantTotal);
        int numRelevantInStreet = (int) (percRelevantInStreet * numRelevantTotal);
        int numRelevantInDistrict = (int) (percRelevantInDistrict * numRelevantTotal);
        int numRelevantInCity = (int) (percRelevantInCity * numRelevantTotal);

        // Calcolo rilevanti generici per ogni area
        int numRelevantGenericInPoI = (int) (percGenericRelevant * numRelevantInPoI);
        int numRelevantGenericInStreet = (int) (percGenericRelevant * numRelevantInStreet);
        int numRelevantGenericInDistrict = (int) (percGenericRelevant * numRelevantInDistrict);
        int numRelevantGenericInCity = (int) (percGenericRelevant * numRelevantInCity);

        // Calcolo rilevanti subevents per ogni area
        int numRelevantSubEventInPoI = numRelevantInPoI - numRelevantGenericInPoI;
        int numRelevantSubEventInStreet = numRelevantInStreet - numRelevantGenericInStreet;
        int numRelevantSubEventInDistrict = numRelevantInDistrict - numRelevantGenericInDistrict;
        int numRelevantSubEventInCity = numRelevantInCity - numRelevantGenericInCity;

        // Calcolo i non rilevanti per ogni area
        int numNotRelevantInPoI = (int) (numRelevantInPoI * (1 - percRelevantInPoI) / percRelevantInPoI);
        int numNotRelevantInStreet = (int) (numRelevantInStreet * (1 - percRelevantInStreet) / percRelevantInStreet);
        int numNotRelevantInDistrict = (int) (numRelevantInDistrict * (1 - percRelevantInDistrict) / percRelevantInDistrict);
        int numNotRelevantInCity = (int) (numRelevantInCity * (1 - percRelevantInCity) / percRelevantInCity);

        System.out.println("\n*** EVENTO " + e.getName() + " ***");
        System.out.println("-> Tweet totali: " + numTotal);
        System.out.println("-> Tweet rilevanti: " + numRelevantTotal);
        System.out.println("-> Tweet non rilevanti: " + numNotRelevantTotal);

        // Genero tweets nelle diverse aree
        generateTweetsInArea(e, e.getEntity(),
                numRelevantSubEventInPoI, numNotRelevantInPoI, numRelevantGenericInPoI);

        generateTweetsInArea(e, ((Poi) e.getEntity()).getStreet(),
                numRelevantSubEventInStreet, numNotRelevantInStreet, numRelevantGenericInStreet);

        generateTweetsInArea(e, ((Poi) e.getEntity()).getDistrict(),
                numRelevantSubEventInDistrict, numNotRelevantInDistrict, numRelevantGenericInDistrict);

        generateTweetsInArea(e, ((Poi) e.getEntity()).getCity(),
                numRelevantSubEventInCity, numNotRelevantInCity, numRelevantGenericInCity);


    }

    private static void generateTweetsInArea(Event e, BaseShape shape,
                                             int numRelevantSubEvents,
                                             int numNotRelevant,
                                             int numGeneric) {

        System.out.println("Preparazione di " + numRelevantSubEvents + " tweet RILEVANTI-SUBEVENTS in AREA " + shape.getLevel());
        System.out.println("Preparazione di " + numGeneric + " tweet RILEVANTI-GENERICI in AREA " + shape.getLevel());
        System.out.println("Preparazione di " + numNotRelevant + " tweet NON RILEVANTI in AREA " + shape.getLevel());

        boolean isInArea = e.getEntity().equals(shape);

        for (int i = 0; i < numRelevantSubEvents; i++) {
            generateSingleTweet(e, shape, true, false, isInArea);
        }
        for (int i = 0; i < numNotRelevant; i++) {
            generateSingleTweet(e, shape, false, false, isInArea);
        }
        for (int i = 0; i < numGeneric; i++) {
            generateSingleTweet(e, shape, true, true, isInArea);
        }
    }

    private static void generateSingleTweet(Event e, BaseShape shape, boolean isRelevant, boolean isGeneric, boolean inArea) {
        // Creo un utente casuale
        if (isGeneric && !isRelevant)
            throw new IllegalArgumentException("Configurazione tweet errata (isRelevant="
                    + isRelevant + ", isGeneric=" + isGeneric + ")");

        String user = "#user" + randomTweets.nextInt(10000);
        ShortTweet tmp = new ShortTweet(user, new Date().toString(), null);
        tmp.setEvent(e);
        tmp.setDate(new Date().toString());
        tmp.setLocation(""); // @ToDo: Verificare che location aggiungere (poi, street, district, city)
        tmp.setGeneric(isGeneric);
        tmp.setRelevant(isRelevant);
        tmp.setInAreaEvent(inArea);

        Point estimatedCoordinates = null;
        if (isRelevant) {
            tmp.setClassLabel(TWEET_CLASS.YES.name());
            if (isGeneric) {
                // Assegno un testo rilevante YES, ma che non riferisce all'evento
                tmp.setText(sentenceYes.get(randomTweets.nextInt(sentenceYes.size())));
            } else {
                // Assegno una descrizione testuale tipica del sotto-evento
                tmp.setText(sentenceYes.get(randomTweets.nextInt(sentenceYes.size())) + " "
                        + getRandomElementFromSet(e.getDescriptions()));
                // Decido che tipo di informazioni testuali da per stimare la geolocalizzazione del tweet
                StringBuilder textualInfo = new StringBuilder();
                double rand = randomTweets.nextDouble();
                if (e.getLevel().equals(ShapeLevel.POI)) {
                    Poi x = (Poi) e.getEntity();
                    if (rand <= percGeoInfoInTextPoi) {
                        textualInfo.append(" " + x.getName());
                        estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getShape(), randomTweets);
                    } else if (rand <= percGeoInfoInTextPoi + percGeoInfoInTextStreet) {
                        textualInfo.append(" " + x.getStreet().getName());
                        estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getStreet().getShape(), randomTweets);
                    } else if (rand <= percGeoInfoInTextPoi + percGeoInfoInTextStreet + percGeoInfoInTextDistrict) {
                        textualInfo.append(" " + x.getDistrict().getName());
                        estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getDistrict().getShape(), randomTweets);
                    } else if (rand <= percGeoInfoInTextPoi + percGeoInfoInTextStreet + percGeoInfoInTextDistrict + percGeoInfoInTextCity) {
                        textualInfo.append(" " + x.getCity().getName());
                        estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getCity().getShape(), randomTweets);
                    }
                } else if (e.getLevel().equals(ShapeLevel.STREET)) {
                    Street x = (Street) e.getEntity();
                    if (rand <= percGeoInfoInTextPoi + percGeoInfoInTextStreet) {
                        textualInfo.append(" " + x.getName());
                        estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getShape(), randomTweets);
                    } else if (rand < percGeoInfoInTextPoi + percGeoInfoInTextStreet + percGeoInfoInTextDistrict) {
                        textualInfo.append(" " + x.getDistrict().getName());
                        estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getDistrict().getShape(), randomTweets);
                    } else if (rand <= percGeoInfoInTextPoi + percGeoInfoInTextStreet + percGeoInfoInTextDistrict + percGeoInfoInTextCity) {
                        textualInfo.append(" " + x.getCity().getName());
                        estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getCity().getShape(), randomTweets);
                    }
                } else if (e.getLevel().equals(ShapeLevel.DISTRICT)) {
                    District x = (District) e.getEntity();
                    if (rand <= percGeoInfoInTextPoi + percGeoInfoInTextStreet + percGeoInfoInTextDistrict) {
                        textualInfo.append(" " + x.getName());
                        estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getShape(), randomTweets);
                    } else if (rand <= percGeoInfoInTextPoi + percGeoInfoInTextStreet + percGeoInfoInTextDistrict + percGeoInfoInTextCity) {
                        textualInfo.append(" " + x.getCity().getName());
                        estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getCity().getShape(), randomTweets);
                    }
                } else {
                    // Livello CITY
                    City x = (City) e.getEntity();
                    textualInfo.append(" " + x.getName());
                    estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getShape(), randomTweets);
                }
                tmp.setText(tmp.getText() + " " + textualInfo);
            }
        } else {
            tmp.setText(tweetNo.get(randomTweets.nextInt(tweetNo.size())).getText());
            tmp.setClassLabel(TWEET_CLASS.NO.name());
        }

        // Aggiungo info di geolocalizzazione relative all'area dell'entità
        Point randomPoint = GeoUtils.getRandomPointInsideShape(shape.getShape(), randomTweets);
        // Per tutti aggiungo una geolocalizzazione hidden (da usare per dbscan)
        tmp.setHiddenLon(randomPoint.getX());
        tmp.setHiddenLat(randomPoint.getY());
        if (randomTweets.nextDouble() <= percGeotagged) {
            // Il punto è geolocalizzato, allora (LAT,LNG)==(LAT_hidden, LON_hidden)
            tmp.setLat(randomPoint.getY());
            tmp.setLon(randomPoint.getX());
        } else {
            if (estimatedCoordinates != null) {
                // Estraggo geo info parziali dalle informazioni di rilevanza
                tmp.setLat(estimatedCoordinates.getY());
                tmp.setLon(estimatedCoordinates.getX());
            } else {
                // NON Geolocalizzato
                tmp.setLat(-1);
                tmp.setLon(-1);
            }
        }

        generatedTweets.add(tmp);

    }

    private static String getRandomElementFromSet(Set<String> set) {
        int index = randomTweets.nextInt(set.size());
        String[] values = set.toArray(new String[0]);
        return values[index];
    }

    private static City csvToCity(String s) {
        try {
            String[] data = s.split(separator);
            Shape shape = getShapeFromCsv(data[1]);
            City c = new City(data[0], shape);
            return c;
        } catch (Exception e) {
            System.err.println("Skipped with errors! " + s);
            return null;
        }
    }

    private static Shape getShapeFromCsv(String s) {
        String shapeInfo[] = s.split(":");
        Shape shape = null;
        switch (shapeInfo[0]) {
            case "CIRCLE": {
                double lat, lon;
                if (isLatLon) {
                    lat = Double.parseDouble(shapeInfo[1]);
                    lon = Double.parseDouble(shapeInfo[2]);
                } else {
                    lat = Double.parseDouble(shapeInfo[2]);
                    lon = Double.parseDouble(shapeInfo[1]);
                }
                double radius = Double.parseDouble(shapeInfo[3]);
                shape = GeoUtils.getCircle(GeoUtils.getPoint(lon, lat), radius);
                break;
            }
            case "RECT": {

                double minX, minY, maxX, maxY;
                if (isLatLon) {
                    minY = Double.parseDouble(shapeInfo[1]);
                    minX = Double.parseDouble(shapeInfo[2]);
                    maxY = Double.parseDouble(shapeInfo[3]);
                    maxX = Double.parseDouble(shapeInfo[4]);
                } else {
                    minY = Double.parseDouble(shapeInfo[2]);
                    minX = Double.parseDouble(shapeInfo[1]);
                    maxY = Double.parseDouble(shapeInfo[4]);
                    maxX = Double.parseDouble(shapeInfo[3]);
                }

                shape = GeoUtils.getRectangle(GeoUtils.getPoint(minX, minY), GeoUtils.getPoint(maxX, maxY));
                break;
            }
            default: {
                throw new InvalidParameterException("Shape District not valid! " + s);
            }
        }
        return shape;
    }

    private static Event csvToEvent(String x) {
        try {
//            ID;TYPE;DESCRIPTION
            String[] data = x.split(separator);
            Event tmp = new Event();
            if (events != null)
                tmp.setName("@event" + events.size());
            tmp.setType(data[1]);
            tmp.setDescriptions(new HashSet<>(Arrays.asList(data[2].split(","))));
            return tmp;
        } catch (Exception e) {
            System.out.println("Skipped with errors! " + x);
            return null;
        }

    }

    private static ShortTweet csvToTweet(String x) {
        String[] data = x.split(separator);
        if (data.length > 2)
            return new ShortTweet(data[0], data[1], data[2]);
        else
            return null;
    }

}
