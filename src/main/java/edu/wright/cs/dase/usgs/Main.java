package edu.wright.cs.dase.usgs;

import static spark.Spark.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;


public class Main {
	
	public static void main(String[] args) {
		
//		ArrayList<Ontology> ontologies = getOntologies();
//		for (Ontology ont: ontologies) {
//			System.out.println(ont);
//		}
		
		ArrayList<Entity> entities = getEntities("USGS.owl");
		for (Entity e: entities) {
			System.out.println(e);
		}

		if (args[0].equals("serve")) {
			port(8080);
			staticFiles.location("/public");
			init();
		
			// get("/ontologies", (req, res) -> getOntologies());
		}
	}
	
	
	// list the ontologies
	public static ArrayList<Ontology> getOntologies() {

		ArrayList<Ontology> ontologies = new ArrayList<>();
		
		File ontDir = new File("./src/main/resources/public/ontologies/");
		
		for (File f: ontDir.listFiles()) {
			
			String filename = f.getName();
			
			if (!filename.endsWith(".owl"))
				continue;
			
			String label = filename.substring(0, filename.indexOf(".owl"));
			
			ontologies.add(new Ontology(label, filename));
		}
		
		return ontologies;
	}

	
	// return all of the entities within an ontology, ordered
	// alphabetically
	public static ArrayList<Entity> getEntities(String ontFilename) {
		
		ArrayList<Entity> entities = new ArrayList<>();
		
		try {
			
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			IRI iri = IRI.create(new File("./src/main/resources/public/ontologies/" + ontFilename));
			OWLOntology ont = manager.loadOntologyFromOntologyDocument(iri);
			OWLDataFactory df = OWLManager.getOWLDataFactory();

			for (OWLClass e : ont.getClassesInSignature()) {
				entities.add(new Entity(
						ontFilename, e.getIRI().toString(), getEntityLabel(e, ont, df)));
			}

			for (OWLObjectProperty e: ont.getObjectPropertiesInSignature()) {
				entities.add(new Entity(
						ontFilename, e.getIRI().toString(), getEntityLabel(e, ont, df)));
			}

			for (OWLDataProperty e: ont.getDataPropertiesInSignature()) {
				entities.add(new Entity(
						ontFilename, e.getIRI().toString(), getEntityLabel(e, ont, df)));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Collections.sort(entities);
		return entities;
	}
	
	
	public static String getEntityLabel(OWLEntity e, OWLOntology ont, OWLDataFactory df) {
		
		String label = e.getIRI().toString();
		String s = "";
		
		Set<OWLAnnotation> annotations = e.getAnnotations(ont, df.getRDFSLabel());
		
		// if there's an rdfs:label property, use it
		if (annotations.size() > 0) {
			OWLAnnotation annotation = annotations.iterator().next();
		
			if (annotation.getValue() instanceof OWLLiteral) {
				OWLLiteral val = (OWLLiteral) annotation.getValue();
				s = val.getLiteral();
			} 
			
		} else { // otherwise, get the label from the end of the URI
			
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
		}
    	
		s = s.toLowerCase();
    	return s.replaceAll("-|_", " ");
	}


	
}
