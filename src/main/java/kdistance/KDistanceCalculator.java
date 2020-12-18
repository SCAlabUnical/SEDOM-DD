package kdistance;

import dbscan.ClusterPoint;
import org.locationtech.spatial4j.shape.Point;
import util.GeoUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class KDistanceCalculator {

	public static double calculateFixedEps(String name, List<Double> distances, double perc) {
		if (perc < 0 || perc > 1)
			throw new RuntimeException("perc deve essere compreso tra 0  e 1");

		int pos = (int) (distances.size() * perc);

		double perCut = pos * 1.0 / distances.size();
		String perCutS = "" + (perCut * 100);

		System.out.println(name + " -> Elbow line= " + (pos) + "/" + distances.size() + " con distanza "
				+ distances.get(pos) + " . Taglio del " + perCutS.substring(0, perCutS.indexOf(".") + 2) + "%");
		return distances.get(pos);
	}

	public static double calculateFirstEps(String name, List<Double> distances) {
		double[] ret = getElbowPoint(distances, 0, distances.size() - 1);

		double perCut = 1.0 * ret[0] / distances.size();
		String perCutS = "" + (perCut * 100);

		System.out.println(name + " -> Elbow line= " + ((int) ret[0]) + "/" + distances.size() + " con distanza "
				+ ret[3] + " . Taglio del " + perCutS.substring(0, perCutS.indexOf(".") + 2) + "%");
		return ret[3];
	}

	public static double[] getElbowPoint(List<Double> distances, int A, int B) {

		if (B - A < 2)
			throw new RuntimeException("Tra A e B ci deve almeno essere un elemento");
		//int dim = B - A + 1;

		// max variables
		int iMax = 0;
		double distMax = 0;
		double xMax = 0;
		double yMax = 0;

		// temporary variables
		double xNorm = -1;
		double yNorm = -1;
		double tmpDist = 0;

		double yB = distances.get(B);
		double yA = distances.get(A);
		double yI = 0.0d;

		double xB = B;
		double xA = A;
		double xI = 0.0d;

		int i = 0;

		for (Iterator<Double> it = distances.iterator(); it.hasNext();) {
			yI = it.next();
			if (i == A) {
				distMax = 0;
				iMax = i;
				xMax = i;
				yMax = yI;
			} else if (i > A && i <= B) {
				xI = i;
				xNorm = (xI - xA) / (xB - xA);
				yNorm = (yI - yB) / (yA - yB);

				tmpDist = ((1.0 - yNorm) - xNorm) * Math.sqrt(2.0) / 2.0;

				if (tmpDist > distMax) {
					distMax = tmpDist;
					iMax = i;
					xMax = xI;
					yMax = yI;
				}
			}
			i++;
		}
		return new double[] { iMax, distMax, xMax, yMax };
	}

	

	public static List<Double> calculateKNN(Collection<ClusterPoint> points, int numNeighbours) {

		List<Double> ret = new LinkedList<Double>();
		double[] mins = new double[numNeighbours];

		double distance = 0.0, tmp = 0.0, tmp2 = 0.0;


		for (Point p1 : points) {
			// reset mins
			for (int i = 0; i < mins.length; i++) {
				mins[i] = Double.MAX_VALUE;
			}

			for (Point p2 : points) {
				if (!p1.equals(p2)) {
					distance = GeoUtils.distance(p1, p2);
					cycle: for (int i = 0; i < mins.length; i++) {
						if (distance < mins[i]) {
							tmp = distance;
							for (int j = i; j < mins.length; j++) {
								tmp2 = mins[j];
								mins[j] = tmp;
								tmp = tmp2;
							} // swap
							mins[i] = distance;
							break cycle;
						} // if con immissione
					} // cycle
				} // if
			} // for p2
			ret.add(mins[numNeighbours - 1]);
		}
		Collections.sort(ret, Collections.reverseOrder());
		return ret;
	}

	 public static double round(double value, int places) {
	 if (places < 0)
	 throw new IllegalArgumentException();
	
	 BigDecimal bd = new BigDecimal(value);
	 bd = bd.setScale(places, RoundingMode.HALF_UP);
	 return bd.doubleValue();
	 }

	
}
