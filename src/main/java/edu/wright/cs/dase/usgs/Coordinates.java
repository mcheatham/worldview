package edu.wright.cs.dase.usgs;

import java.util.ArrayList;

public class Coordinates {

	private ArrayList<Point> points;
	
	public Coordinates() {
		points = new ArrayList<>();
	}
	
	public void addPoint(double lng, double lat) {
		points.add(new Point(lng, lat));
	}
	
	public Point getFirst() {
		return points.get(0);
	}
	
	@Override
	public String toString() {
		String s = "";
		for (Point p: points) {
			s += p.toString() + " ";
		}
		return s.trim();
	}
}
