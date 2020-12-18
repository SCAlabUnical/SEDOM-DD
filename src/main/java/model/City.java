package model;

import org.locationtech.spatial4j.shape.Shape;

public class City extends BaseShape {

    public City(String name) {
        super(name, ShapeLevel.CITY);
    }

    public City(String name, Shape shape) {
        super(name, shape, ShapeLevel.CITY);
    }
}
