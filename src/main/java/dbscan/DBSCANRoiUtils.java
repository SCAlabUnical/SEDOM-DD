package dbscan;

import org.apache.commons.math3.ml.clustering.Cluster;
import org.locationtech.spatial4j.shape.Shape;
import util.GeoUtils;
import util.KMLUtils;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DBSCANRoiUtils {

    public static void storeClusterKML(final List<Cluster<ClusterPoint>> clusters, String outputPath) {

        Shape tmpShape = null;
        Map<String, String> ext = null;
        try {
            PrintWriter pw = new PrintWriter(
                    new File(outputPath));
            pw.println(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document>");
            int i = 0;
            for (Cluster<ClusterPoint> cluster : clusters) {
                ext = new HashMap<String, String>();
                ext.put("name", "cluster n." + i);
                i++;
                tmpShape = GeoUtils.convexHull(
                        cluster.getPoints().stream()
                                .map(cp-> GeoUtils.getPoint(cp.getX(),cp.getY()))
                                .collect(Collectors.toList())
                );
                pw.println(KMLUtils.serialize(tmpShape, false, ext));
            }
            pw.println("</Document></kml>");
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }




}
