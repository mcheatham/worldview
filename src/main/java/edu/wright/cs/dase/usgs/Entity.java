package edu.wright.cs.dase.usgs;

import java.io.Serializable;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

public class Entity implements Comparable<Entity>, Serializable {
	
	private static final long serialVersionUID = 1L;
	
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
		return URI + " (" + label + ") in " + ontology;
	}

	@Override
	public int compareTo(Entity o) {
		return label.compareTo(o.label);
	}
	
	
	public static String getEntityLabel(OWLClassExpression e) {
		if (e.isAnonymous()) return e.toString();
		return getEntityLabel((OWLNamedObject) e);
	}
	
	
	public static String getEntityLabel(OWLDataPropertyExpression e) {
		if (e.isAnonymous()) return e.toString();
		return getEntityLabel((OWLNamedObject) e);
	}
	
	
	public static String getEntityLabel(OWLObjectPropertyExpression e) {
		if (e.isAnonymous()) return e.toString();
		return getEntityLabel((OWLNamedObject) e);
	}
	
	
	public static String getEntityLabel(OWLNamedObject e) {
		
		String label = e.getIRI().toString();
		String s = "";

		if (label.contains("#")) {
			label = label.substring(label.indexOf('#')+1);
		}

		if (label.contains("/")) {
			label = label.substring(label.lastIndexOf('/')+1);
		}

		// break up words (camelCase)
		s += label.charAt(0);

		for (int i=1; i<label.length(); i++) {

			if (Character.isUpperCase(label.charAt(i)) && 
					!Character.isUpperCase(label.charAt(i-1))) {
				s += " ";
			} 

			s += label.charAt(i);
		}

		s = s.toLowerCase();
		return s.replaceAll("-|_", "");
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((URI == null) ? 0 : URI.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Entity other = (Entity) obj;
		if (URI == null) {
			if (other.URI != null)
				return false;
		} else if (!URI.equals(other.URI))
			return false;
		return true;
	}
}
