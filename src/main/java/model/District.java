package model;

import org.locationtech.spatial4j.shape.Shape;

public class District extends BaseShape {

    private City city;

    public District(String name) {
        super(name, ShapeLevel.DISTRICT);
    }

    public District(String name, Shape shape) {
        super(name, shape, ShapeLevel.DISTRICT);
    }

    public District(String name, Shape shape, City city) {
        super(name, shape, ShapeLevel.DISTRICT);
        this.city = city;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }
}
