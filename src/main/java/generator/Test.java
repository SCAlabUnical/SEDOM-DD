package generator;

import util.GeoUtils;

public class Test {
    public static void main(String[] args) {
        double d = (GeoUtils.getDistanceMeters(GeoUtils.getPoint(40.0,10.0),GeoUtils.getPoint(41.0,10.0)));
        System.out.println(1.0/d);
        System.out.println(0.001171044423423/100.0);


    }
}
