package edu.wright.cs.dase.usgs;

import java.util.ArrayList;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;

public class Axiom {
	
	private String text;
	private String owl;
	private ArrayList<Entity> entities = new ArrayList<>();

	public Axiom(OWLAxiom ax, String ontId) {
		this.owl = ax.toString();
		this.entities = extractEntities(ax, ontId);
		this.text = ax.toString(); // TODO may use OWLtoEnglish converter later
	}
	
	
	private ArrayList<Entity> extractEntities(OWLAxiom ax, String ontId) {
		
		ArrayList<Entity> entities = new ArrayList<>();
		
		for (OWLClass cls: ax.getClassesInSignature()) {
			entities.add(new Entity(ontId, cls));
		}
		
		return entities;
	}
	
	
	private static String getAxiomType(OWLAxiom ax) {
		
		String relation = null;
		
		AxiomType<?> type = ax.getAxiomType();
		
		if (type == AxiomType.EQUIVALENT_CLASSES || type == AxiomType.EQUIVALENT_DATA_PROPERTIES 
				|| type == AxiomType.EQUIVALENT_OBJECT_PROPERTIES) {
			relation = "is the same as";
			
		} else if (type == AxiomType.SUBCLASS_OF || type == AxiomType.SUB_DATA_PROPERTY 
				|| type == AxiomType.SUB_OBJECT_PROPERTY) {
			relation = "is a type of";
			
		} else { // TODO handle other axiom types
			System.err.println("Unhandled axiom type: " + ax.toString());
		}
		
		return relation;
	}
	
	
	public static boolean isInteresting(OWLAxiom ax) {
		return getAxiomType(ax) != null;
	}

	
	@Override
	public String toString() {
		String s = text + "\n";
		for (Entity e: entities) {
			s += "\t" + e + "\n";
		}
		return s;
	}
	

	public String getText() {
		return text;
	}
	
	
	public String getOWL() {
		return owl;
	}
	
	
	public ArrayList<Entity> getEntities() {
		return entities;
	}
}
