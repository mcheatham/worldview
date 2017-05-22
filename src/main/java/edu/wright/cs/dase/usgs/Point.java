package edu.wright.cs.dase.usgs;

public class Point {

	private double lat;
	private double lng;
	
	public Point(double lat, double lng) {
		this.lat = lat;
		this.lng = lng;
	}
	
	public double getLat() {
		return lat;
	}
	
	public double getLng() {
		return lng;
	}
	
	public double getDistanceFrom(Point o) {
		return Math.sqrt(Math.pow(this.lat-o.lat, 2) + Math.pow(this.lng-o.lng, 2));
	}
	
	@Override
	public String toString() {
		return "(" + lat + "," + lng + ")";
	}
}
