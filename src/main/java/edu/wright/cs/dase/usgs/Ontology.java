package edu.wright.cs.dase.usgs;

public class Ontology {
	
	private String label;
	private String identifier;
	
	public Ontology(String label, String identifier) {
		this.label = label;
		this.identifier = identifier;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getIdentifier() {
		return identifier;
	}
	
	@Override
	public String toString() {
		return identifier + " (" + label + ")";
	}

}
