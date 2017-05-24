package edu.wright.cs.dase.usgs;

import org.semanticweb.owlapi.model.OWLClassExpression;

public class Entity implements Comparable<Entity> {
	
	private String ontology;
	private String URI;
	private String label;
	private OWLClassExpression entity;
	
	public Entity(String ontology, OWLClassExpression ent) {
		this.ontology = ontology;
		this.URI = ent.toString();
		this.label = getEntityLabel(ent);
		this.entity = ent;
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
	
	public OWLClassExpression getEntity() {
		return entity;
	}

	@Override
	public String toString() {
		return URI + " (" + label + ") in " + ontology;
	}

	@Override
	public int compareTo(Entity o) {
		return label.compareTo(o.label);
	}
	
	private String getEntityLabel(OWLClassExpression e) {
		
		if (!e.isClassExpressionLiteral()) return e.toString();
		
		String label = e.asOWLClass().getIRI().toString();
		String s = "";
		
//		Set<OWLAnnotation> annotations = e.getAnnotations(ont, df.getRDFSLabel());
//		
//		// if there's an rdfs:label property, use it
//		if (annotations.size() > 0) {
//			OWLAnnotation annotation = annotations.iterator().next();
//		
//			if (annotation.getValue() instanceof OWLLiteral) {
//				OWLLiteral val = (OWLLiteral) annotation.getValue();
//				s = val.getLiteral();
//			} 
//			
//		} else { // otherwise, get the label from the end of the URI
			
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
//		}
    	
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
