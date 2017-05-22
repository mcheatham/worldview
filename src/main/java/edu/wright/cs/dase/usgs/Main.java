package edu.wright.cs.dase.usgs;

import static spark.Spark.init;
import static spark.Spark.port;
import static spark.Spark.staticFiles;

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
	
	private static HashMap<String, OWLOntology> ontologies;
	private static OWLOntologyManager manager;

	
	public static void main(String[] args) {
		
		ontologies = new HashMap<>();
		manager = OWLManager.createOWLOntologyManager();
		
//		ArrayList<Ontology> ontologies = getOntologies();
//		for (Ontology ont: ontologies) {
//			System.out.println(ont);
//		}
		
//		ArrayList<Entity> entities = getEntities("USGS.owl");
//		for (Entity e: entities) {
//			System.out.println(e);
//		}
		
		ArrayList<Axiom> axioms = getAxioms(
				"http://spatial.maine.edu/semgaz/HydroOntology#Coastline", 
				"Hydro3.owl", "USGS.owl");
		for (Axiom a: axioms) {
			System.out.println(a);
		}
		
		axioms = getAxioms(
				"http://spatial.maine.edu/semgaz/HydroOntology#Wetlands", 
				"Hydro3.owl", "USGS.owl");
		for (Axiom a: axioms) {
			System.out.println(a);
		}

		if (args.length > 0 && args[0].equals("serve")) {
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

	
	// return all of the entities within an ontology, ordered alphabetically
	public static ArrayList<Entity> getEntities(String ontFilename) {
		
		ArrayList<Entity> entities = new ArrayList<>();

		OWLOntology ont = getOntology(ontFilename, false);

		for (OWLClass e : ont.getClassesInSignature()) {
			entities.add(new Entity(ontFilename, e));
		}

		for (OWLObjectProperty e: ont.getObjectPropertiesInSignature()) {
			entities.add(new Entity(ontFilename, e));
		}

		for (OWLDataProperty e: ont.getDataPropertiesInSignature()) {
			entities.add(new Entity(ontFilename, e));
		}

		Collections.sort(entities);
		return entities;
	}


	// return all of the axioms involving the ontology pair that contain a particular entity
	public static ArrayList<Axiom> getAxioms(String entityURI, String ont1Filename, String ont2Filename) {
		
		ArrayList<Axiom> axioms = new ArrayList<>();
		
		String alignmentFilename = ont1Filename.replaceAll(".owl", "") + "-" + 
				ont2Filename.replaceAll(".owl", "") + ".owl";
		
		OWLOntology alignmentOnt = getOntology(alignmentFilename, true);
		OWLOntology ont2 = getOntology(ont2Filename, false);
		
		for (OWLAxiom ax: alignmentOnt.getAxioms()) {
			axioms.add(new Axiom(ax, ont1Filename, ont2Filename, ont2));
		}
		
		return axioms;
	}

	
	// TODO
	public static HashMap<Entity, ArrayList<Coordinates>> getCoordinates(String axiomOWL, 
			String ont1Filename, String ont2Filename, double lat, double lng, int limit) {
		
		HashMap<Entity, ArrayList<Coordinates>> entityCoordinates = new HashMap<>();
		
		
		
		return entityCoordinates;
	}
	
	
	private static OWLOntology getOntology(String ontFilename, boolean isAlignment) {
		
		OWLOntology ont = null;
		
		String path = "./src/main/resources/public/ontologies/";
		if (isAlignment) {
			path = "./src/main/resources/public/alignments/";
		}
		
		if (ontologies.containsKey(ontFilename)) {
			ont = ontologies.get(ontFilename);
			
		} else {
			
			try {

				if (ontologies.size() > 5) {
					ontologies.clear(); // don't let the cache get too big
				}
				
				IRI iri = IRI.create(new File(path + ontFilename));
				ont = manager.loadOntologyFromOntologyDocument(iri);
				ontologies.put(ontFilename, ont);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return ont;
	}
}

