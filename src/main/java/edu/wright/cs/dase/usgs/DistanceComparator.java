package edu.wright.cs.dase.usgs;

import java.util.Comparator;

public class DistanceComparator implements Comparator<Coordinates> {

	private Point refLocation;
	
	public DistanceComparator(double lat, double lng) {
		refLocation = new Point(lat, lng);
	}
	
	
	@Override
	// compares based on first point in each set of coordinates
	public int compare(Coordinates o1, Coordinates o2) {
		
		Point p1 = o1.getFirst();
		Point p2 = o2.getFirst();
		
		if (p1 == null) {
			if (p2 == null) {
				return 0;
			} else {
				return 1;
			}
			
		} else if (p2 == null) {
			return -1;
			
		} else {
			double d1 = p1.getDistanceFrom(refLocation);
			double d2 = p2.getDistanceFrom(refLocation);
			
			if (d1 < d2) {
				return -1;
			} else if (d1 > d2) {
				return 1;
			} else {
				return 0;
			}
		}
	}

}
