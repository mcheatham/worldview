package edu.wright.cs.dase.usgs;

public class Similarity implements Comparable<Similarity> {
	
	private Entity ent;
	private double sim;
	
	public Similarity(Entity ent, double sim) {
		this.ent = ent;
		this.sim = sim;
	}

	@Override
	public int compareTo(Similarity o) {
		if (sim > o.sim) {
			return -1;
		} else if (sim < o.sim) {
			return 1;
		} else {
			return ent.getLabel().compareTo(o.ent.getLabel());
		}
	}
	
	public Entity getEntity() {
		return ent;
	}
	
	public double getSim() {
		return sim;
	}
}
