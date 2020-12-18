package dbscan;

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.impl.PointImpl;


public class ClusterPoint extends PointImpl implements Clusterable  {
	
	public ClusterPoint(double x, double y, SpatialContext ctx) {
		super(x, y, ctx);
	}

	public ClusterPoint(String x, String y, SpatialContext ctx) {
		super(Double.parseDouble(x), Double.parseDouble(y), ctx);
	}

	@Override
	public double[] getPoint() {
		double[] p = {this.getX(), this.getY()};
		return p;
	}
	
	
	

}
