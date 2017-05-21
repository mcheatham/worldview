package edu.wright.cs.dase.usgs;

public class Coordinates {

	private double lat;
	private double lng;
	
	public Coordinates(double lat, double lng) {
		this.lat = lat;
		this.lng = lng;
	}
	
	public double getLat() {
		return lat;
	}
	
	public double getLng() {
		return lng;
	}
	
	@Override
	public String toString() {
		return "(" + lat + "," + lng + ")";
	}
}
