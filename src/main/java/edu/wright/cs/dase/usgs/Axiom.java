package edu.wright.cs.dase.usgs;

import java.util.ArrayList;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;

public class Axiom {
	
	private String text;
	private String owl;
	private ArrayList<Entity> entities = new ArrayList<>();
	private OWLAxiom axiom;

	
	public Axiom(OWLAxiom ax, String ont1Filename, String ont2Filename, 
			OWLOntology ont2) {
		this.axiom = ax;
		this.owl = ax.toString();
		this.text = ax.toString(); // TODO may use OWLtoEnglish converter later
		extractEntities(ont1Filename, ont2Filename, ont2);
	}
	
	
	// TODO this needs work -- only classes are currently supported
	private void extractEntities(String ont1Filename, 
			String ont2Filename, OWLOntology ont2) {

		Set<OWLClassExpression> expressions = axiom.getNestedClassExpressions();
		for (OWLClassExpression expr: expressions) {

			String ontFilename = ont2Filename;
			for (OWLClass cls: expr.getClassesInSignature()) {
				if (!ont2.containsEntityInSignature(cls))
					ontFilename = ont1Filename;
			}

			entities.add(new Entity(ontFilename, expr));
		}
	}
	
	
	// TODO this may need work to support a wider variety of axiom types
	public static boolean isInteresting(OWLAxiom ax) {
		AxiomType<?> type = ax.getAxiomType();
		
		if (type == AxiomType.EQUIVALENT_CLASSES || type == AxiomType.EQUIVALENT_DATA_PROPERTIES 
				|| type == AxiomType.EQUIVALENT_OBJECT_PROPERTIES)
			return true;
			
		if (type == AxiomType.SUBCLASS_OF || type == AxiomType.SUB_DATA_PROPERTY 
				|| type == AxiomType.SUB_OBJECT_PROPERTY) 
			return true;
			
		return false;
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
		ArrayList<Entity> temp = new ArrayList<>();
		temp.addAll(entities);
		return temp;
	}
	
	
	public OWLAxiom getOWLAxiom() {
		return axiom;
	}
}
