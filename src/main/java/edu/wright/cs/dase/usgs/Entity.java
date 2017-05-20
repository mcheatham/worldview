package edu.wright.cs.dase.usgs;

public class Entity implements Comparable<Entity> {
	
	private String ontology;
	private String URI;
	private String label;
	
	public Entity(String ontology, String URI, String label) {
		this.ontology = ontology;
		this.URI = URI;
		this.label = label;
	}

	public String getOntology() {
		return ontology;
	}

	public String getURI() {
		return URI;
	}

	public String getLabel() {
		return label;
	}

	@Override
	public String toString() {
		return URI + " (" + label + ")";
	}

	@Override
	public int compareTo(Entity o) {
		return label.compareTo(o.label);
	}
}
