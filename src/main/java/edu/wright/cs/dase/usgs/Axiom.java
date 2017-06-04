package edu.wright.cs.dase.usgs;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.owlxml.renderer.OWLXMLObjectRenderer;
import org.semanticweb.owlapi.owlxml.renderer.OWLXMLWriter;

public class Axiom {
	
	private String text;
	private String owl;
	private ArrayList<Entity> entities = new ArrayList<>();
	private OWLAxiom axiom;

	
	public Axiom(OWLAxiom ax, OWLOntology ont, String ont1Filename, String ont2Filename, 
			OWLOntology ont2) {
		this.axiom = ax;
		this.owl = getOWL(ax, ont);
		this.text = ax.toString(); // TODO may use OWLtoEnglish converter later
		extractEntities(ont1Filename, ont2Filename, ont2);
	}
	
	
	private void extractEntities(String ont1Filename, 
			String ont2Filename, OWLOntology ont2) {

		Set<OWLClassExpression> expressions = axiom.getNestedClassExpressions();
		for (OWLClassExpression expr: expressions) {

			String ontFilename = ont2Filename;
			for (OWLClass cls: expr.getClassesInSignature()) {
				if (!ont2.containsEntityInSignature(cls))
					ontFilename = ont1Filename;
			}

			entities.add(new ClassEntity(ontFilename, expr));
		}
	}
	
	
	public static boolean isInteresting(OWLAxiom ax) {
		AxiomType<?> type = ax.getAxiomType();
		
		if (type == AxiomType.EQUIVALENT_CLASSES)
			return true;
			
		if (type == AxiomType.SUBCLASS_OF) 
			return true;
		
		if (type == AxiomType.DISJOINT_CLASSES) 
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
	
	
	public static String getOWL(OWLAxiom ax, OWLOntology ont) {
		
		StringWriter stringWriter = new StringWriter();
		OWLXMLWriter writer = new OWLXMLWriter(stringWriter, ont);
		OWLXMLObjectRenderer renderer = new OWLXMLObjectRenderer(writer);
		
		if (ax instanceof OWLEquivalentClassesAxiom) {
			renderer.visit((OWLEquivalentClassesAxiom) ax);

		} else if (ax instanceof OWLSubClassOfAxiom) {
			renderer.visit((OWLSubClassOfAxiom) ax);

		} else if (ax instanceof OWLDisjointClassesAxiom) {
			renderer.visit((OWLDisjointClassesAxiom) ax);

		} else {
			return ax.toString();
		}

		return stringWriter.toString().trim().replaceAll(" ", "").replaceAll("\n", "");
	}
}
