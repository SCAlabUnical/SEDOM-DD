package model;

import org.locationtech.spatial4j.shape.Shape;

public class Street extends BaseShape {

    private District district;

    public Street(String name, District district) {
        super(name, ShapeLevel.STREET);
        this.district = district;
    }

    public Street(String name, Shape shape, District district) {
        super(name, shape, ShapeLevel.STREET);
        this.district = district;
    }

    public District getDistrict() {
        return district;
    }

    public void setDistrict(District district) {
        this.district = district;
    }

    public City getCity() {
        return this.district.getCity();
    }
}
