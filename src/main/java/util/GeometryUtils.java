package util;

import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.*;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.*;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GeometryUtils {


	public static double getIntersectionArea(Shape a, Shape b) {
		Geometry ga = ((JtsGeometry) a).getGeom();
		Geometry gb = ((JtsGeometry) b).getGeom();
		return ga.intersection(gb).getArea();
	}

	public static double getIntersectionArea(Geometry a, Geometry b) {
		return a.intersection(b).getArea();
	}

	public static Geometry getConvexHull(Point... points) {
		Coordinate[] coordinates = new Coordinate[points.length];
		for (int i = 0; i < coordinates.length; i++) {
			coordinates[i] = new Coordinate(points[i].getX(), points[i].getY());
		}
		GeometryFactory gf = new GeometryFactory();
		ConvexHull ch = new ConvexHull(coordinates, gf);
		return ch.getConvexHull();
	}

	public static Shape getShape(Geometry g) {
		return new JtsGeometry(g, JtsSpatialContext.GEO, false, false);
	}

	public static String serialize(Geometry geometry, boolean closeFile, Map<String, String> ext) throws IOException {
		if (geometry instanceof Point) {
			return serializePoint((Point) geometry, closeFile, ext);
		} else if (geometry instanceof LineString) {
			// return serializeLineString((LineString) geometry);
		} else if (geometry instanceof Polygon) {
			return serializePolygon((Polygon) geometry, closeFile, ext);
		} else {
			throw new IllegalArgumentException("Geometry type [" + geometry.getGeometryType() + "] not supported");
		}
		return null;
	}
	
	private static String serializePoint(Point geometry, boolean closeFile, Map<String, String> extendedData) {
		StringBuilder sb = new StringBuilder();
		if (closeFile) {
			sb.append(
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?><kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document>");
		}
		HashMap<String, String> ext = generateExtendedDataString(extendedData);
		sb.append("<Placemark>" + ext.get("data") + "<Point><coordinates>" + geometry.getX() + "," + geometry.getY()
				+ "</coordinates></Point></Placemark>");
		if (closeFile) {
			sb.append("</Document></kml>");
		}
		return sb.toString();
	}

	private static String serializePolygon(Polygon geometry, boolean closeFile, Map<String, String> extendedData) {

		StringBuilder sb = new StringBuilder();

		if (closeFile) {
			sb.append(
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?><kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document>");
		}

		HashMap<String, String> ext = generateExtendedDataString(extendedData);
		sb.append("<Placemark>" + ext.get("data")
				+ "<Polygon><outerBoundaryIs><LinearRing><tessellate>0</tessellate><coordinates>");
		Coordinate[] coordinates = geometry.getCoordinates();
		List<Coordinate> pointsList = new LinkedList<Coordinate>();

		double sumLat = 0;
		double sumLng = 0;
		for (Coordinate coordinate : coordinates) {
			pointsList.add(coordinate);
			sumLat += coordinate.y;
			sumLng += coordinate.x;
		}
		Coordinate reference = new Coordinate(sumLng / coordinates.length, sumLat / coordinates.length);
		Collections.sort(pointsList, new ClockwiseCoordinateComparator(reference));
		Coordinate tmp = pointsList.remove(coordinates.length - 1);
		pointsList.add(0, tmp);
		for (Coordinate c : pointsList) {
			sb.append(c.x + "," + c.y + ",0.0 ");
		}
		sb.append("</coordinates></LinearRing></outerBoundaryIs></Polygon></Placemark>");

		if (ext.containsKey("style"))
			sb.append(ext.get("style"));

		if (closeFile) {
			sb.append("</Document></kml>");
		}

		return sb.toString();
	}
	
	private static HashMap<String, String> generateExtendedDataString(Map<String, String> extendedData) {
		String ext = "";
		String preText = "";
		HashMap<String, String> ret = new HashMap<String, String>();
		if (extendedData != null && extendedData.size() > 0) {
			ext = "<ExtendedData>";
			for (Map.Entry<String, String> entry : extendedData.entrySet()) {
				if (entry.getKey().equals("styleUrl") || entry.getKey().equals("description")) {
					preText += "<styleUrl>" + entry.getValue().trim() + "</styleUrl>";
				} else if (entry.getKey().equals("color")) {
					preText += "<styleUrl>#poly-" + entry.getValue().trim() + "</styleUrl>";
					String style = "<Style id=\"poly-" + entry.getValue().trim() + "\">" + "<LineStyle>" + "<color>"
							+ entry.getValue().trim() + "</color>" + "	<width>2</width>" + "</LineStyle>"
							+ "<PolyStyle>" + "<color>" + entry.getValue().trim() + "</color>" + "	<fill>1</fill>"
							+ "<outline>1</outline>" + "</PolyStyle></Style>";
					ret.put("style", style);

				} else if (entry.getKey().equals("description")) {
					preText += "<description><![CDATA[descrizione:" + entry.getValue() + "]]></description>";
				} else if (entry.getKey().equals("name")) {
					preText += "<name>" + entry.getValue() + "</name>";
				} else {
					ext += "<Data name=\"" + entry.getKey() + "\"><value>" + entry.getValue() + "</value></Data>";
				}
			}
			ext += "</ExtendedData>";
		}
		ret.put("data", preText + ext);
		return ret;
	}

	
	public static List<Geometry> loadShape(String path) {

		List<Geometry> shapes = new LinkedList<Geometry>();

		Kml obj = Kml.unmarshal(new File(path));

		Document document = (Document) obj.getFeature();
		for (Feature feature : document.getFeature()) {

			Folder folder;
			LinkedList<Feature> features = new LinkedList<Feature>();
			if (feature instanceof Folder) {
				folder = (Folder) feature;
				features.addAll(folder.getFeature());
			}
			if (feature instanceof Placemark) {
				features.add(feature);
			}

			for (Feature f : features) {
				Placemark placemark = (Placemark) f;

				de.micromata.opengis.kml.v_2_2_0.Polygon poly = (de.micromata.opengis.kml.v_2_2_0.Polygon) placemark
						.getGeometry();
				Boundary boundary = poly.getOuterBoundaryIs();
				LinearRing linear = boundary.getLinearRing();
				Shape shape = GeoUtils.getPolygonOpenGISCoordinate(true, linear.getCoordinates());
				shapes.add(((JtsGeometry) shape).getGeom());

			}
		}
		return shapes;

	}
	
	public static List<Geometry> loadShapesFromKMLString(String content) {

		List<Geometry> shapes = new LinkedList<Geometry>();

		Kml obj = Kml.unmarshal(content);

		Document document = (Document) obj.getFeature();
		for (Feature feature : document.getFeature()) {

			Folder folder;
			LinkedList<Feature> features = new LinkedList<Feature>();
			if (feature instanceof Folder) {
				folder = (Folder) feature;
				features.addAll(folder.getFeature());
			}
			if (feature instanceof Placemark) {
				features.add(feature);
			}

			for (Feature f : features) {
				Placemark placemark = (Placemark) f;

				de.micromata.opengis.kml.v_2_2_0.Polygon poly = (de.micromata.opengis.kml.v_2_2_0.Polygon) placemark
						.getGeometry();
				Boundary boundary = poly.getOuterBoundaryIs();
				LinearRing linear = boundary.getLinearRing();
				Shape shape = GeoUtils.getPolygonOpenGISCoordinate(true, linear.getCoordinates());
				shapes.add(((JtsGeometry) shape).getGeom());

			}
		}
		return shapes;

	}
	
	public static Map<String,Geometry> loadShapeMap(String path) {

		Map<String,Geometry> shapes = new HashMap<String,Geometry>();
		
		Kml obj = Kml.unmarshal(new File(path));

		Document document = (Document) obj.getFeature();
		for (Feature feature : document.getFeature()) {

			Folder folder;
			LinkedList<Feature> features = new LinkedList<Feature>();
			if (feature instanceof Folder) {
				folder = (Folder) feature;
				features.addAll(folder.getFeature());
			}
			if (feature instanceof Placemark) {
				features.add(feature);
			}

			for (Feature f : features) {
				Placemark placemark = (Placemark) f;
				de.micromata.opengis.kml.v_2_2_0.Polygon poly = (de.micromata.opengis.kml.v_2_2_0.Polygon) placemark
						.getGeometry();
				Boundary boundary = poly.getOuterBoundaryIs();
				LinearRing linear = boundary.getLinearRing();
				Shape shape = GeoUtils.getPolygonOpenGISCoordinate(true, linear.getCoordinates());
				shapes.put(placemark.getName(),((JtsGeometry) shape).getGeom());

			}
		}
		return shapes;

	}
}
