package util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class Roi {
	private Shape shape; // Convex hull
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Shape getShape() {
		return shape;
	}

	public void setShape(Shape shape) {
		this.shape = shape;
	}

	public Coordinate[] getConvexHullPoints() {
		JtsGeometry gg = (JtsGeometry) shape;
		return gg.getGeom().getCoordinates();
	}

	public double getAreaSquaredKm() {
		return (GeoUtils.getAreaSquaredKm(shape));
	}

	public double getAreaSquaredHa() {
		return (GeoUtils.getAreaSquaredKm(shape) * 100);
	}

	public String toKML(boolean closeFile) {
		StringBuilder s = new StringBuilder();
		Map<String, String> map = new HashMap<String, String>();
		map.put("name", name);
		try {
			s.append(KMLUtils.serialize(shape, closeFile, map));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return s.toString();
	}


}
