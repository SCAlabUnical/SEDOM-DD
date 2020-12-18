package generator;

import model.*;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;
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

public class Generator {

    enum TWEET_CLASS {YES, NO}

    private static ResourceBundle genProp = ResourceBundle.getBundle("config.generator");
    private static Random random = new Random();
    private static List<String> sentenceYes;
    private static List<ShortTweet> tweetNo;
    private static List<ShortTweet> generatedTweets = new LinkedList<>();
    private static Map<String, Street> streets = new HashMap<>();
    private static Map<String, District> districts = new HashMap<>();
    private static Map<String, City> cities = new HashMap<>();
    private static Map<String, Poi> pois = new HashMap();
    // Carico paramentri di configurazione
    private static int numMinRelevantTweetPerEvent;
    private static int numMaxRelevantTweetPerEvent;
    private static double percGeotagged;
    private static double percRelevantInArea;
    private static double percGenericRelevant;
    private static double ratioNotRelevant;
    private static boolean isLatLon;
    private static double percGeoInfoPoi;
    private static double percGeoInfoStreet;
    private static double percGeoInfoDistrict;
    private static double percGeoInfoCity;

    public static void main(String[] args) throws IOException {

        System.out.println("*** START GENERATOR ***");

        // Carico parametri di configurazione
        numMinRelevantTweetPerEvent = Integer.parseInt(genProp.getString("numMinRelevantTweetPerEvent"));
        numMaxRelevantTweetPerEvent = Integer.parseInt(genProp.getString("numMaxRelevantTweetPerEvent"));
        percGeotagged = Double.parseDouble(genProp.getString("percGeotagged"));
        percRelevantInArea = Double.parseDouble(genProp.getString("percRelevantInArea"));
        percGenericRelevant = Double.parseDouble(genProp.getString("percGenericRelevant"));
        ratioNotRelevant = Double.parseDouble(genProp.getString("ratioNotRelevant"));
        isLatLon = Boolean.parseBoolean(genProp.getString("LatLon"));

        percGeoInfoPoi = Double.parseDouble(genProp.getString("percGeoInfoPoi"));
        percGeoInfoStreet = Double.parseDouble(genProp.getString("percGeoInfoStreet"));
        percGeoInfoDistrict = Double.parseDouble(genProp.getString("percGeoInfoDistrict"));
        percGeoInfoCity = Double.parseDouble(genProp.getString("percGeoInfoCity"));

        // Carico file CSV per generazione eventi
        sentenceYes = Files.lines(Paths.get("src/main/resources/sentenceYes.csv")).skip(1)
                .filter(Objects::nonNull).collect(Collectors.toList());
        tweetNo = Files.lines(Paths.get("src/main/resources/tweetNo.csv")).skip(1)
                .map(Generator::csvToTweet).filter(Objects::nonNull).collect(Collectors.toList());

        cities = Files.lines(Paths.get("src/main/resources/cities.csv")).skip(1)
                .map(Generator::csvToCity).filter(Objects::nonNull)
                .collect(Collectors.toMap(City::getName, Function.identity()));

        districts = Files.lines(Paths.get("src/main/resources/districts.csv")).skip(1)
                .map(Generator::csvToDistrict).filter(Objects::nonNull)
                .collect(Collectors.toMap(District::getName, Function.identity()));

        streets = Files.lines(Paths.get("src/main/resources/streets.csv")).skip(1)
                .map(Generator::csvToStreet).filter(Objects::nonNull)
                .collect(Collectors.toMap(Street::getName, Function.identity()));

        pois = Files.lines(Paths.get("src/main/resources/pois.csv")).skip(1)
                .map(Generator::csvToPoi).filter(Objects::nonNull)
                .collect(Collectors.toMap(Poi::getName, Function.identity()));

        List<Event> events = Files.lines(Paths.get("src/main/resources/events.csv"))
                .skip(1).map(Generator::csvToEvent).filter(Objects::nonNull).collect(Collectors.toList());

        // Genero Tweets per ciascun evento
        events.forEach(e -> generateTweets(e));

        generateEventKMLs(events);

        List<String> tweetsText = generatedTweets.stream().map(t -> t.toString()).collect(Collectors.toList());
        tweetsText.add(0, "USER;DATE;TEXT;LOCATION;LAT;LON;CLASS;HIDDEN_LAT;HIDDEN_LON;RELEVANT;GENERIC");
        Files.write(Paths.get(genProp.getString("pathOutput") + "generatedDataset.csv"), tweetsText, StandardCharsets.UTF_8);

        List<String> tweetsKML = generatedTweets.stream().map(t -> tweetToKML(t, true)).filter(Objects::nonNull).collect(Collectors.toList());
        storeInFile(tweetsKML, "generatedDataset.kml");

        List<String> tweetsRelevantKML = generatedTweets.stream().filter(t -> t.isRelevant())
                .map(t -> tweetToKML(t, true)).collect(Collectors.toList());
        storeInFile(tweetsRelevantKML, "generatedRelevantDataset.kml");

        List<String> tweetsSubEventRelevantKML = generatedTweets.stream().filter(t -> t.isSubEventRelevant())
                .map(t -> tweetToKML(t, true)).collect(Collectors.toList());
        storeInFile(tweetsSubEventRelevantKML, "generatedSubEventRelevantDataset.kml");

        List<String> tweetsGenericKML = generatedTweets.stream().filter(t -> t.isGeneric())
                .map(t -> tweetToKML(t,true)).collect(Collectors.toList());
        storeInFile(tweetsGenericKML, "generatedGenericDataset.kml");


        List<String> tweetsNotRelevantKML = generatedTweets.stream().filter(t -> !t.isRelevant())
                .map(t -> tweetToKML(t, true)).collect(Collectors.toList());
        storeInFile(tweetsNotRelevantKML, "generatedNotRelevantDataset.kml");

        // Preparo dataset per DBSCAN
        List<String> tweetsYesCoordinates = generatedTweets.stream().filter(t -> t.isSubEventRelevant())
                .map(x -> extractCoordinates(x)).collect(Collectors.toList());

        Files.write(Paths.get(genProp.getString("pathOutput") + "dbscanDB.csv"), tweetsYesCoordinates, StandardCharsets.UTF_8);

        List<String> tweetsDBSCANCoordinatesKML = tweetsYesCoordinates.stream()
                .map(x -> {
                    String separator = genProp.getString("csvSeparator");
                    try {
                        return KMLUtils.serialize(GeoUtils.getPoint(x, separator, isLatLon));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());
        storeInFile(tweetsDBSCANCoordinatesKML, "dbscanDB.kml");

        System.out.println("*** DATI KML ***");
        System.out.println("Tweet complessivi: " + generatedTweets.size());
        System.out.println("Rilevanti: " + tweetsRelevantKML.size());
        System.out.println("\t->Generici: " + tweetsGenericKML.size());
        System.out.println("\t->Sottoeventi: " + tweetsSubEventRelevantKML.size());
        System.out.println("Non Rilevanti: " + tweetsNotRelevantKML.size());
        System.out.println("DBSCAN: " + tweetsDBSCANCoordinatesKML.size());



    }

    private static void storeInFile(List<String> data, String s) throws IOException {
        data.add(0, KMLUtils.OPEN_TAGS);
        data.add(KMLUtils.CLOSE_TAGS);
        Files.write(Paths.get(genProp.getString("pathOutput") + s), data, StandardCharsets.UTF_8);
        data.remove(0);
        data.remove(data.size() - 1);
    }

    private static String extractCoordinates(ShortTweet x) {
        if (x.getLat() > 0 && x.getLon() > 0) {
            if (isLatLon) {
                return x.getLat() + genProp.getString("csvSeparator") + x.getLon();
            } else {
                return x.getLon() + genProp.getString("csvSeparator") + x.getLat();
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

        Files.write(Paths.get(genProp.getString("pathOutput") + "generatedPois.kml"), poisKML, StandardCharsets.UTF_8);
        Files.write(Paths.get(genProp.getString("pathOutput") + "generatedStreets.kml"), streetsKML, StandardCharsets.UTF_8);
        Files.write(Paths.get(genProp.getString("pathOutput") + "generatedDistricts.kml"), districtsKML, StandardCharsets.UTF_8);
        Files.write(Paths.get(genProp.getString("pathOutput") + "generatedCities.kml"), citiesKML, StandardCharsets.UTF_8);

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
        System.out.println("*** EVENTO " + e.getName() + " ***");

        // Numero di tweet in area evento (PoI)
        int numRelevantTotal = numMinRelevantTweetPerEvent + random.nextInt(numMaxRelevantTweetPerEvent - numMinRelevantTweetPerEvent);
        int numRelevantInArea = (int) (percRelevantInArea * numRelevantTotal);
        int numGenericInArea = (int) (percGenericRelevant * numRelevantInArea);
        int numRelevantSubEventsInArea = numRelevantInArea - numGenericInArea;
        int numNotRelevantInArea = (int) (numRelevantInArea * ratioNotRelevant);

        // Numero di tweet esterni all'area (costruiti a livello city)
        int numRelevantExternal = numRelevantTotal - numRelevantInArea;
        int numGenericExternal = (int) (percGenericRelevant * numRelevantExternal);
        int numRelevantSubEventsExternal = numRelevantExternal - numGenericExternal;
        int numNotRelevantExternal = (int) (ratioNotRelevant * numRelevantExternal);

        // Genero tweets nell'area dell'evento (PoI)
        System.out.println("Preparazione di " + numRelevantSubEventsInArea + " tweet RILEVANTI-SUBEVENTS in AREA " + e.getLevel());
        System.out.println("Preparazione di " + numGenericInArea + " tweet RILEVANTI-GENERICI in AREA " + e.getLevel());
        System.out.println("Preparazione di " + numNotRelevantInArea + " tweet NON RILEVANTI in AREA " + e.getLevel());
        generateTweetsInArea(e, e.getEntity(), numRelevantSubEventsInArea, numNotRelevantInArea, numGenericInArea);

        // Creo tweet rilevanti, non rilevanti e generici fuori dall'area dell'evento (al momento solo CITY)
        BaseShape city = null;
        if (e.getLevel() == ShapeLevel.POI) {
            city = ((Poi) e.getEntity()).getCity();
        } else if (e.getLevel() == ShapeLevel.STREET) {
            city = ((Street) e.getEntity()).getCity();
        } else if (e.getLevel() == ShapeLevel.DISTRICT) {
            city = ((District) e.getEntity()).getCity();
        } else if (e.getLevel() == ShapeLevel.CITY) {
            city = e.getEntity();
        }

        System.out.println("Preparazione di " + numRelevantSubEventsExternal + " tweet RILEVANTI-SUBEVENTS in EXTERNAL AREA " + ShapeLevel.CITY);
        System.out.println("Preparazione di " + numGenericExternal + " tweet RILEVANTI-GENERICI in EXTERNAL AREA " + ShapeLevel.CITY);
        System.out.println("Preparazione di " + numNotRelevantExternal + " tweet NON RILEVANTI in EXTERNAL AREA " + ShapeLevel.CITY);
        generateTweetsInArea(e, city, numRelevantSubEventsExternal, numNotRelevantExternal, numGenericExternal);

    }

    private static void generateTweetsInArea(Event e, BaseShape shape,
                                             int numRelevantSubEvents,
                                             int numNotRelevant,
                                             int numGeneric) {

        for (int i = 0; i < numRelevantSubEvents; i++) {
            generateSingleTweet(e, shape, true, false);
        }
        for (int i = 0; i < numNotRelevant; i++) {
            generateSingleTweet(e, shape, false, false);
        }
        for (int i = 0; i < numGeneric; i++) {
            generateSingleTweet(e, shape, true, true);
        }
    }

    private static void generateSingleTweet(Event e, BaseShape shape, boolean isRelevant, boolean isGeneric) {
        // Creo un utente casuale
        if (isGeneric && !isRelevant)
            throw new IllegalArgumentException("Configurazione tweet errata (isRelevant="
                    + isRelevant + ", isGeneric=" + isGeneric + ")");

        String user = "#user" + random.nextInt(10000);
        ShortTweet tmp = new ShortTweet(user, new Date().toString(), null);
        tmp.setDate(new Date().toString());
        tmp.setLocation(""); // @ToDo: Verificare che location aggiungere (poi, street, district, city)
        tmp.setGeneric(isGeneric);
        tmp.setRelevant(isRelevant);

        Point estimatedCoordinates = null;
        if (isRelevant) {
            tmp.setClassLabel(TWEET_CLASS.YES.name());
            if (isGeneric) {
                // Assegno un testo rilevante YES, ma che non riferisce all'evento
                tmp.setText(sentenceYes.get(random.nextInt(sentenceYes.size())));
            } else {
                // Assegno una descrizione testuale tipica del sotto-evento
                tmp.setText(sentenceYes.get(random.nextInt(sentenceYes.size())) + " "
                        + getRandomElementFromSet(e.getDescriptions()));
                // Decido che tipo di informazioni testuali da per stimare la geolocalizzazione del tweet
                StringBuilder textualInfo = new StringBuilder();
                double rand = random.nextDouble();
                if (e.getLevel().equals(ShapeLevel.POI)) {
                    Poi x = (Poi) e.getEntity();
                    if (rand < percGeoInfoPoi) {
                        textualInfo.append(" " + x.getName());
                        estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getShape());
                    } else if (rand < percGeoInfoPoi + percGeoInfoStreet) {
                        textualInfo.append(" " + x.getStreet().getName());
                        estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getStreet().getShape());
                    } else if (rand < percGeoInfoPoi + percGeoInfoStreet + percGeoInfoDistrict) {
                        textualInfo.append(" " + x.getDistrict().getName());
                        estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getDistrict().getShape());
                    } else {
                        textualInfo.append(" " + x.getCity().getName());
                        estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getCity().getShape());
                    }
                } else if (e.getLevel().equals(ShapeLevel.STREET)) {
                    Street x = (Street) e.getEntity();
                    if (rand < percGeoInfoPoi + percGeoInfoStreet) {
                        textualInfo.append(" " + x.getName());
                        estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getShape());
                    } else if (rand < percGeoInfoPoi + percGeoInfoStreet + percGeoInfoDistrict) {
                        textualInfo.append(" " + x.getDistrict().getName());
                        estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getDistrict().getShape());
                    } else {
                        textualInfo.append(" " + x.getCity().getName());
                        estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getCity().getShape());
                    }
                } else if (e.getLevel().equals(ShapeLevel.DISTRICT)) {
                    District x = (District) e.getEntity();
                    if (rand < percGeoInfoPoi + percGeoInfoStreet + percGeoInfoDistrict) {
                        textualInfo.append(" " + x.getName());
                        estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getShape());
                    } else {
                        textualInfo.append(" " + x.getCity().getName());
                        estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getCity().getShape());
                    }
                } else {
                    // Livello CITY
                    City x = (City) e.getEntity();
                    textualInfo.append(" " + x.getName());
                    estimatedCoordinates = GeoUtils.getRandomPointInsideShape(x.getShape());
                }
                tmp.setText(tmp.getText() + " " + textualInfo);
            }
        } else {
            tmp.setText(tweetNo.get(random.nextInt(tweetNo.size())).getText());
            tmp.setClassLabel(TWEET_CLASS.NO.name());
        }

        // Aggiungo info di geolocalizzazione relative all'area dell'entità
        Point randomPoint = GeoUtils.getRandomPointInsideShape(shape.getShape());
        // Per tutti aggiungo una geolocalizzazione hidden (da usare per dbscan)
        tmp.setHiddenLon(randomPoint.getX());
        tmp.setHiddenLat(randomPoint.getY());
        if (random.nextDouble() < percGeotagged) {
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
        int index = random.nextInt(set.size());
        String[] values = set.toArray(new String[0]);
        return values[index];
    }

    private static City csvToCity(String s) {
//      CITY;SHAPE
        try {
            String[] data = s.split(genProp.getString("csvSeparator"));
            Shape shape = getShapeFromCsv(data[1]);
            City c = new City(data[0], shape);
            return c;
        } catch (Exception e) {
            System.err.println("Skipped with errors! " + s);
            return null;
        }
    }

    private static District csvToDistrict(String s) {
        try {
            String[] data = s.split(genProp.getString("csvSeparator"));
            Shape shape = getShapeFromCsv(data[2]);
            City city = cities.get(data[1]);
            if (city == null)
                throw new InvalidParameterException("The city of the district is not valid! " + s);
            District d = new District(data[0], shape, city);
            return d;
        } catch (Exception e) {
            System.err.println("Skipped with errors! " + s);
            return null;
        }
    }

    private static Street csvToStreet(String s) {
//        STREET;DISTRICT;SHAPE
        try {
            String[] data = s.split(genProp.getString("csvSeparator"));
            Shape shape = getShapeFromCsv(data[2]);
            District district = districts.get(data[1]);
            Street st = new Street(data[0], shape, district);
            return st;
        } catch (Exception e) {
            System.err.println("Skipped with errors! " + s);
            return null;
        }
    }

    private static Poi csvToPoi(String s) {
        try {
            String[] data = s.split(genProp.getString("csvSeparator"));
            Shape shape = getShapeFromCsv(data[2]);
            Street currentStreet = streets.get(data[1]);
            if (currentStreet == null)
                throw new InvalidParameterException("The street of the poi is not valid! " + s);
            Poi p = new Poi(data[0], shape, currentStreet);
            return p;
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
//            EVENT;LEVEL;ENTITY;TYPE;DESCRIPTION
            String[] data = x.split(genProp.getString("csvSeparator"));
            Event tmp = new Event();
            tmp.setName(data[0]);
            switch (data[1]) {
                case "POI": {
                    tmp.setEntity(pois.get(data[2]));
                    break;
                }
                case "STREET": {
                    tmp.setEntity(streets.get(data[2]));
                    break;
                }
                case "DISTRICT": {
                    tmp.setEntity(districts.get(data[2]));
                    break;
                }
                case "CITY": {
                    tmp.setEntity(cities.get(data[2]));
                    break;
                }

            }
            tmp.setType(data[3]);
            tmp.setDescriptions(new HashSet<>(Arrays.asList(data[4].split(","))));
            return tmp;
        } catch (Exception e) {
            System.out.println("Skipped with errors! " + x);
            return null;
        }

    }

    private static ShortTweet csvToTweet(String x) {
        String[] data = x.split(genProp.getString("csvSeparator"));
        if (data.length > 2)
            return new ShortTweet(data[0], data[1], data[2]);
        else
            return null;
    }

}
