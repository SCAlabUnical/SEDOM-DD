package generator;

import dbscan.ClusterPoint;
import dbscan.DBSCANRoiUtils;
import dbscan.LngLatDistance;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.locationtech.spatial4j.context.SpatialContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class GeneratorDBScanDataset {

    private static ResourceBundle genProp = ResourceBundle.getBundle("config.generator");


    public static void main(String[] args) throws IOException {
        String separator = genProp.getString("csvSeparator");
        boolean isLatLon = Boolean.parseBoolean(genProp.getString("LatLon"));
        Collection<ClusterPoint> points = Files.lines(Paths.get("dbscanDB.csv")).skip(1)
                .map(x -> {
                    String[] data = x.split(separator);
                    if (isLatLon) {
                        return new ClusterPoint(data[1], data[0], SpatialContext.GEO);
                    } else {
                        return new ClusterPoint(data[0], data[1], SpatialContext.GEO);
                    }
                }).collect(Collectors.toList());

        /** Guessing EPS **/
        int DBSCAN_MIN_PTS = 4;
        double DBSCAN_EPSILON = 55;

        DBSCANClusterer<ClusterPoint> mydbscan = new DBSCANClusterer<ClusterPoint>(
                DBSCAN_EPSILON, DBSCAN_MIN_PTS - 1, new LngLatDistance());
        List<Cluster<ClusterPoint>> clusters = mydbscan.cluster(points);

        DBSCANRoiUtils.storeClusterKML(
                clusters.stream().filter(c -> c.getPoints().size() >= 10).collect(Collectors.toList()),
                "DBSCAN_clusters.kml");

    }



}
