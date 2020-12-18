package dbscan;

import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.ArrayAdapterDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.geo.LngLatDistanceFunction;
import de.lmu.ifi.dbs.elki.math.geodesy.SphericalHaversineEarthModel;
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

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ElkiDBSCAN {

    double[][] dataElki;
    LngLatDistanceFunction dist = new LngLatDistanceFunction(SphericalHaversineEarthModel.STATIC);
    DatabaseConnection dbc;
    Database db;
    Relation<NumberVector> rel;
    DBIDRange ids;
    DBSCAN<NumberVector> dbscan;
    Clustering<Model> results;
    double eps;
    int minPts;

    public ElkiDBSCAN(List<String> points, double eps, int minPts, String separator, boolean isLatLon) {
        this.eps = eps;
        this.minPts = minPts;
        this.dataElki = new double[points.size()][2];
        int i = 0;
        for (String t : points) {
            String coords[] = t.split(separator);
            double lng, lat;
            if (isLatLon) {
                lng = Double.parseDouble(coords[1]);
                lat = Double.parseDouble(coords[0]);
            } else {
                lng = Double.parseDouble(coords[0]);
                lat = Double.parseDouble(coords[1]);
            }
            dataElki[i][0] = lng;
            dataElki[i][1] = lat;
            i++;
        }
        dbc = new ArrayAdapterDatabaseConnection(dataElki);
        db = new StaticArrayDatabase(dbc, null);
        db.initialize();
        rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
        ids = (DBIDRange) rel.getDBIDs();
        dbscan = new DBSCAN<NumberVector>(dist, this.eps, this.minPts);

    }

    public void cluster() {
        results = dbscan.run(db);
    }

    public void clustersToKML(String outputPath, boolean includeNoise) {
        Shape tmpShape = null;
        Map<String, String> ext = null;

        try {
            PrintWriter pw = new PrintWriter(
                    new File(outputPath));
            pw.println(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document>");
            int i = 0;
            for (Cluster cluster : this.results.getAllClusters()) {

                if (!includeNoise && cluster.isNoise())
                    continue;

                ext = new HashMap<>();
                ext.put("name", "cluster n." + i);
                i++;
                tmpShape = getConvexHull(cluster);
                pw.println(KMLUtils.serialize(tmpShape, false, ext));
            }
            pw.println("</Document></kml>");
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Shape getConvexHull(Cluster cluster) {
        GeometryFactory geometryFactory = new GeometryFactory();
        List<Coordinate> coords = new LinkedList<>();

        for (DBIDIter it = cluster.getIDs().iter(); it.valid(); it.advance()) {
            final int offset = ids.getOffset(it);
            coords.add(new Coordinate(dataElki[offset][0], dataElki[offset][1]));
        }

        MultiPoint mp = geometryFactory.createMultiPointFromCoords(coords.toArray(new Coordinate[0]));
        Geometry geoHull = mp.convexHull();
        return new JtsGeometry(geoHull, JtsSpatialContext.GEO, false, false);
    }

    public List<Cluster<Model>> getAllClusters() {
        return results.getAllClusters();
    }

    public List<GeoCluster> getAllGeoClusters(boolean includeNoise) {
        List<GeoCluster> res = new LinkedList<>();
        for (Cluster cluster : this.results.getAllClusters()) {
            if (!includeNoise && cluster.isNoise())
                continue;
            List<Point> coords = new LinkedList<>();
            for (DBIDIter it = cluster.getIDs().iter(); it.valid(); it.advance()) {
                final int offset = ids.getOffset(it);
                coords.add(GeoUtils.getPoint(dataElki[offset][0], dataElki[offset][1]));
            }
            res.add(new GeoCluster(coords));
        }
        return res;
    }

}
