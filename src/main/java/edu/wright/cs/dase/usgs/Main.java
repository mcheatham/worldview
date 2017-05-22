package edu.wright.cs.dase.usgs;

import static spark.Spark.*;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;


public class Main {
	
	private static OWLOntology alignmentOnt;
	private static String alignmentFilename;
	private static String ont1Filename;
	private static String ont2Filename;
	private static OWLOntology mergedOntology;
	
	public static void main(String[] args) {
		
//		ArrayList<Ontology> ontologies = getOntologies();
//		for (Ontology ont: ontologies) {
//			System.out.println(ont);
//		}
		
//		ArrayList<Entity> entities = getEntities("USGS.owl");
//		for (Entity e: entities) {
//			System.out.println(e);
//		}
		
//		ArrayList<Axiom> axioms = getAxioms(
//				"http://spatial.maine.edu/semgaz/HydroOntology#Coastline", 
//				"Hydro3.owl", "USGS.owl");
//		for (Axiom a: axioms) {
//			System.out.println(a);
//		}
//		
//		axioms = getAxioms(
//				"http://spatial.maine.edu/semgaz/HydroOntology#Wetlands", 
//				"Hydro3.owl", "USGS.owl");
//		for (Axiom a: axioms) {
//			System.out.println(a);
//		}

		Gson gson = new Gson();

		if (args.length > 0 && args[0].equals("serve")) {
			port(8080);
			staticFiles.location("/public");
			init();
		
			get("/ontologies", (reqest, response) -> getOntologies(), gson::toJson);

			get("/entities", (request, response) -> {
				return getEntities(request.queryParams("ontology"));
			}, gson::toJson);

			get("/axioms", (request, response) -> {
				String entity = request.queryParams("entity");
				String ont1 = request.queryParams("ontology1");
				String ont2 = request.queryParams("ontology2");
				return getAxioms(entity, ont1, ont2);
			}, gson::toJson);
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

			for (OWLClass e : ont.getClassesInSignature()) {
				entities.add(new Entity(ontFilename, e));
			}

			for (OWLObjectProperty e: ont.getObjectPropertiesInSignature()) {
				entities.add(new Entity(ontFilename, e));
			}

			for (OWLDataProperty e: ont.getDataPropertiesInSignature()) {
				entities.add(new Entity(ontFilename, e));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Collections.sort(entities);
		return entities;
	}


	// return all of the axioms involving the ontology pair that contain a particular entity
	public static ArrayList<Axiom> getAxioms(String entityURI, String ont1Filename, String ont2Filename) {
		
		ArrayList<Axiom> axioms = new ArrayList<>();
		
		String axiomFilename = ont1Filename.replaceAll(".owl", "") + "-" + 
				ont2Filename.replaceAll(".owl", "") + ".owl";
		
		// only read in the alignment ontology if we're using a different alignment 
		// than the last time this method was called.
		if (!axiomFilename.equals(alignmentFilename) || alignmentOnt != null) {
			try {

				OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
				IRI iri = IRI.create(new File("./src/main/resources/public/alignments/" + axiomFilename));
				alignmentOnt = manager.loadOntologyFromOntologyDocument(iri);
				alignmentFilename = axiomFilename;

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		for (OWLAxiom ax: alignmentOnt.getAxioms()) {
			if (ax.toString().contains(entityURI) && Axiom.isInteresting(ax)) {
				axioms.add(new Axiom(ax, ont1Filename));
			}
		}
		
		return axioms;
	}

	
//	public static HashMap<Entity, ArrayList<Coordinates>> getCoordinates(String axiomOWL, 
//			String ont1Filename, String ont2Filename, double lat, double lng, int limit) {
//		
//		// Create a merged ontology with everything from ont1 and ont2 (or make a copy of an existing one)
//		if (!ont1Filename.equals(alignmentFilename) || alignmentOnt != null) {
//			try {
//
//				OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
//				IRI iri = IRI.create(new File("./src/main/resources/public/alignments/" + axiomFilename));
//				alignmentOnt = manager.loadOntologyFromOntologyDocument(iri);
//				alignmentFilename = axiomFilename;
//
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		} else {
//			
//		}
//		// Add in the axiom
//		// Invoke a reasoner
//		// Get all of the entities from the axiom
//		// For each one, get the coordinates all of the instances of that entity from the ontology (cache these) 
//		// Sort these by distance from the lat, lng
//		// Add the closest "limit" instances to an ArrayList
//		// Put the ArrayList for this entity into the HashMap
//		// Return the result
//		
//	}

}

