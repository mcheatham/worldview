package edu.wright.cs.dase.usgs;

import static spark.Spark.init;
import static spark.Spark.port;
import static spark.Spark.staticFiles;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
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
	private static String baseOntology = "USGS.owl"; // must have coordinate data
	private static String sparqlEndpoint = "http://10.0.1.35:3030/CEGIS/query";

	
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
		
		HashMap<Entity, ArrayList<Coordinates>> coords = getCoordinates(
				"EquivalentClasses(<http://cegis.usgs.gov/SWO/SwampOrMarsh> <http://spatial.maine.edu/semgaz/HydroOntology#Wetlands> )", 
				"Hydro3.owl", "USGS.owl", 75.0, 75.0, 10);
		for (Entity e: coords.keySet()) {
			System.out.println(e);
			for (Coordinates c: coords.get(e)) {
				System.out.println("\t" + c);
			}
		}

//		Gson gson = new Gson();
//
//		if (args.length > 0 && args[0].equals("serve")) {
//			port(8080);
//			staticFiles.location("/public");
//			init();
//		
//			get("/ontologies", (reqest, response) -> getOntologies(), gson::toJson);
//
//			get("/entities", (request, response) -> {
//				return getEntities(request.queryParams("ontology"));
//			}, gson::toJson);
//
//			get("/axioms", (request, response) -> {
//				String entity = request.queryParams("entity");
//				String ont1 = request.queryParams("ontology1");
//				String ont2 = request.queryParams("ontology2");
//				return getAxioms(entity, ont1, ont2);
//			}, gson::toJson);
//		}
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
	
	
	// return an Axiom object based on this OWL statement
	public static Axiom getAxiom(String axiomOWL, String ont1Filename, String ont2Filename) {
		
		String alignmentFilename = ont1Filename.replaceAll(".owl", "") + "-" + 
				ont2Filename.replaceAll(".owl", "") + ".owl";
		
		OWLOntology alignmentOnt = getOntology(alignmentFilename, true);
		OWLOntology ont2 = getOntology(ont2Filename, false);
		
		for (OWLAxiom ax: alignmentOnt.getAxioms()) {
			if (ax.toString().equals(axiomOWL)) {
				return new Axiom(ax, ont1Filename, ont2Filename, ont2);
			}
		}
		
		return null;
	}

	
	// TODO
	public static HashMap<Entity, ArrayList<Coordinates>> getCoordinates(String axiomOWL, 
			String ont1Filename, String ont2Filename, double lat, double lng, int limit) {
		
		HashMap<Entity, ArrayList<Coordinates>> entityCoordinates = new HashMap<>();
		
		// get an Axiom object using the OWL statement
		Axiom theAxiom = getAxiom(axiomOWL, ont1Filename, ont2Filename);
		
		// get all of the entities in this axiom
		ArrayList<Entity> entities = theAxiom.getEntities();
		
		// for each entity
		for (Entity ent: entities) {
		
			ArrayList<Coordinates> coordinates = null;
			
			// if ent is in the base ontology, query Fuseki for the coordinates of all instances
			// if ent is not in the base ontology, query based on the second half of the axiom
			String query = null;
			
			if (ent.getOntology().equals(baseOntology)) {
	        
	            query = "SELECT ?shape WHERE {"
	            	  + "?instance a " + ent.getURI() + " . " 
	            	  + "?instance <http://www.opengis.net/ont/geosparql#hasGeometry> ?geometry . "
	            	  + "?geometry <http://www.opengis.net/ont/geosparql#asWKT> ?shape "
	            	  + "}";
	            
	            coordinates = getCoordinates(query);
	            entityCoordinates.put(ent, coordinates);
				
			} else {
				// TODO
			}
			
			if (coordinates == null) continue;
			
			// sort the instances based on the distance between each instance and the location
			Collections.sort(coordinates, new DistanceComparator(lat, lng));
		
			// collect the first "limit" into the ArrayList
			ArrayList<Coordinates> keepers = new ArrayList<>();
			for (int i=0; i<limit; i++) {
				keepers.add(coordinates.get(i));
			}
			entityCoordinates.put(ent, keepers);
		}
		
		return entityCoordinates;
	}
	
	
	private static ArrayList<Coordinates> getCoordinates(String query) {
		
        System.out.println(query);
        
        ArrayList<Coordinates> coordinatesList = new ArrayList<>();
        
        QueryExecution qe = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);

        ResultSet results = qe.execSelect();
        while (results.hasNext()) {
        	QuerySolution result = results.next();
        	String shape = result.getLiteral("?shape").toString();
        	
        	Coordinates coordinates = new Coordinates();
        	shape = shape.substring(shape.indexOf("(")+2, shape.indexOf(")"));
        	String[] pairs = shape.split(",");
        	for (String pair: pairs) {
        		String[] values = pair.trim().split("[ ]");
        		double lat = Double.parseDouble(values[0].trim());
        		double lng = Double.parseDouble(values[1].trim());
        		coordinates.addPoint(lat, lng);
        	}
        	coordinatesList.add(coordinates);
        }
        qe.close();
        
        return coordinatesList;
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

