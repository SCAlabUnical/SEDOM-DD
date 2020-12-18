package model;

import org.locationtech.spatial4j.shape.Shape;

public class BaseShape {
    String name;
    Shape shape;
    ShapeLevel level;

    public BaseShape(String name, ShapeLevel level) {
        this.level = level;
        this.name = name;
    }

    public BaseShape(String name, Shape shape, ShapeLevel level) {
        this.name = name;
        this.shape = shape;
        this.level = level;
    }

    public ShapeLevel getLevel() {
        return level;
    }

    public void setLevel(ShapeLevel level) {
        this.level = level;
    }

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

    public double getLatCenter() {
        return shape.getCenter().getY();
    }

    public double getLonCenter() {
        return shape.getCenter().getX();
    }

}