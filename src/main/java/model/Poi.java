package model;

import org.locationtech.spatial4j.shape.Shape;

public class Poi extends BaseShape {
    private Street street;

    public Poi(String name) {
        super(name, ShapeLevel.POI);
    }

    public Poi(String name, Shape shape) {
        super(name, shape, ShapeLevel.POI);
    }

    public Poi(String name, Shape shape, Street street) {
        super(name, shape, ShapeLevel.POI);
        this.street = street;
    }

    public District getDistrict() {
        return street.getDistrict();
    }

    public City getCity() {
        return street.getDistrict().getCity();
    }

    public Street getStreet() {
        return street;
    }

    public void setStreet(Street street) {
        this.street = street;
    }

}
