package edu.wright.cs.dase.usgs;

import org.semanticweb.owlapi.model.OWLEntity;

public class Entity implements Comparable<Entity> {
	
	private String ontology;
	private String URI;
	private String label;
	
	public Entity(String ontology, OWLEntity ent) {
		this.ontology = ontology;
		this.URI = ent.toString();
		this.label = getEntityLabel(ent);
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
	
	private String getEntityLabel(OWLEntity e) {
		
		String label = e.getIRI().toString();
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
    	return s.replaceAll("-|_", " ");
	}
}
