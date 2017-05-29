package edu.wright.cs.dase.usgs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.google.gson.Gson;

import static spark.Spark.*;

import org.aksw.owl2sparql.OWLClassExpressionToSPARQLConverter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;

public class Main {
	
	private static HashMap<String, OWLOntology> ontologies = new HashMap<>();
	private static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	private static String baseOntology = "USGS.owl"; // must have coordinate data
	private static String sparqlEndpoint = "http://10.0.1.35:3030/CEGIS/query";
	private static HashMap<String, ArrayList<Entity>> relations;
	private static String ont1Relations = "";
	private static String ont2Relations = "";
	private static double synWeight = 0.0;
	private static double semWeight = 0.0;
	private static double structWeight = 0.0;

	
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
//				"http://spatial.maine.edu/semgaz/HydroOntology#Watershed", 
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
		
//		HashMap<Entity, ArrayList<Coordinates>> coords = getCoordinates(
//				"SubClassOf(<http://spatial.maine.edu/semgaz/HydroOntology#Watershed> "
//				+ "ObjectUnionOf(<http://cegis.usgs.gov/SWO/LakeOrPond> <http://cegis.usgs.gov/SWO/SwampOrMarsh>))", 
//				"Hydro3.owl", "USGS.owl", 75.0, 75.0, 10);
//		for (Entity e: coords.keySet()) {
//			System.out.println(e);
//			for (Coordinates c: coords.get(e)) {
//				System.out.println("\t" + c);
//			}
//		}

		ArrayList<ClassEntity> entities = getClasses("Hydro3.owl");
		for (Entity e: entities) {
			System.out.println(e);
			
			ArrayList<Entity> relatedEntities = getRelatedClasses(e.getURI(), "Hydro3.owl", "USGS.owl", .2, .5, .3);
			for (int i=0; i<3; i++) {
				System.out.println("\t" + relatedEntities.get(i));
			}
		}

		if (args.length > 0 && args[0].equals("serve")) {
			Gson gson = new Gson();

			port(8080);
			staticFiles.location("/public");
			init();
		
			get("/ontologies", (reqest, response) -> {
				return getOntologies();
			}, gson::toJson);

			get("/classes", (request, response) -> {
				return getClasses(request.queryParams("ontology"));
			}, gson::toJson);
			
			get("/properties", (request, response) -> {
				return getProperties(request.queryParams("ontology"));
			}, gson::toJson);

			get("/axioms", (request, response) -> {
				String cls = request.queryParams("class");
				String ont1 = request.queryParams("ontology1");
				String ont2 = request.queryParams("ontology2");
				return getAxioms(cls, ont1, ont2);
			}, gson::toJson);

			get("/coordinates", (request, response) -> {
				String axiom = request.queryParams("axiom");
				String ont1 = request.queryParams("ontology1");
				String ont2 = request.queryParams("ontology2");
				Double lat = new Double(request.queryParams("lat"));
				Double lng = new Double(request.queryParams("lng"));
				return getCoordinates(axiom, ont1, ont2, lat, lng, 10);
			}, gson::toJson);

			get("/relatedClasses", (request, response) -> {
				String cls = request.queryParams("class");
				String ont1 = request.queryParams("ontology1");
				String ont2 = request.queryParams("ontology2");
				Double syn = new Double(request.queryParams("syn"));
				Double sem = new Double(request.queryParams("sem"));
				Double struct = new Double(request.queryParams("struct"));
				return getRelatedClasses(cls, ont1, ont2, syn, sem, struct);
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

	
	// return all of the entities within an ontology, ordered alphabetically
	public static ArrayList<ClassEntity> getClasses(String ontFilename) {
		
		ArrayList<ClassEntity> entities = new ArrayList<>();

		OWLOntology ont = getOntology(ontFilename, false);

		for (OWLClass e : ont.getClassesInSignature()) {
			entities.add(new ClassEntity(ontFilename, e));
		}

		Collections.sort(entities);
		return entities;
	}
	
	
	// return all of the entities within an ontology, ordered alphabetically
	public static ArrayList<PropertyEntity> getProperties(String ontFilename) {
		
		ArrayList<PropertyEntity> entities = new ArrayList<>();

		OWLOntology ont = getOntology(ontFilename, false);

		for (OWLObjectProperty e : ont.getObjectPropertiesInSignature()) {
			entities.add(new PropertyEntity(ontFilename, e));
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
			if (ax.toString().contains(entityURI))
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
	
	
	public static ArrayList<Entity> getRelatedClasses(String entityURI, String ont1, String ont2, 
			double syn, double sem, double struct) {
		if (relations == null || !ont1Relations.equals(ont1) || !ont2Relations.equals(ont2) || 
				synWeight != syn || semWeight != sem || structWeight != struct) {
			relations = AutomatedAlignment.getSimilarities(ont1, ont2, syn, sem, struct);
			ont1Relations = ont1;
			ont2Relations = ont2;
			synWeight = syn;
			semWeight = sem;
			structWeight = struct;
		}
		return relations.get(entityURI);
	}

	
	// return a list of the limit closest entities relevant to the axiom as measured from 
	// the provided latitude and longitude
	public static HashMap<Entity, ArrayList<Coordinates>> getCoordinates(String axiomOWL, 
			String ont1Filename, String ont2Filename, double lat, double lng, int limit) {
		
		HashMap<Entity, ArrayList<Coordinates>> entityCoordinates = new HashMap<>();
		
		// get an Axiom object using the OWL statement
		Axiom theAxiom = getAxiom(axiomOWL, ont1Filename, ont2Filename);
		
		// get all of the entities in this axiom
		ArrayList<Entity> entities = theAxiom.getEntities();
		
		OWLClassExpressionToSPARQLConverter converter = new OWLClassExpressionToSPARQLConverter();
		
		// for each entity
		for (Entity ent: entities) {
		
			ArrayList<Coordinates> coordinates = null;
			
			// if ent is in the base ontology, query Fuseki for the coordinates of all instances
			String query = null;
			
			if (ent.getOntology().equals(baseOntology)) {

				if (ent instanceof PropertyEntity) continue;

				OWLClassExpression ce = ((ClassEntity) ent).getEntity();
				Query q = converter.asQuery(ce, "?x");

				PrefixMapping pm = new PrefixMappingImpl();
				if(q.toString().contains("rdf-schema#")) {
					pm.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
				}
				if(q.toString().contains("rdf-syntax-ns#")) {
					pm.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
				}
				if(q.toString().contains("XMLSchema#")) {
					pm.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
				}
				q.setPrefixMapping(pm);

				query = q.toString();
				query = query.replace("SELECT DISTINCT  ?x", "SELECT DISTINCT  ?shape");
				query = query.substring(0, query.lastIndexOf("}"));
				query +=  ". ?x <http://www.opengis.net/ont/geosparql#hasGeometry> ?geometry . "
						+ "?geometry <http://www.opengis.net/ont/geosparql#asWKT> ?shape . } LIMIT 1000";

				coordinates = getCoordinates(query);
				entityCoordinates.put(ent, coordinates);
			}
			
			if (coordinates == null) continue;
			
			// sort the instances based on the distance between each instance and the location
			Collections.sort(coordinates, new DistanceComparator(lat, lng));
		
			// collect the first "limit" into the ArrayList
			ArrayList<Coordinates> keepers = new ArrayList<>();
			int numToAdd = Math.min(coordinates.size(), limit);
			for (int i=0; i<numToAdd; i++) {
				keepers.add(coordinates.get(i));
			}
			entityCoordinates.put(ent, keepers);
		}
		
		return entityCoordinates;
	}
	
	
	private static ArrayList<Coordinates> getCoordinates(String query) {
        
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
        		coordinates.addPoint(lng, lat);
        	}
        	coordinatesList.add(coordinates);
        }
        qe.close();
        
        return coordinatesList;
	}
	
	
	public static OWLOntology getOntology(String ontFilename, boolean isAlignment) {
		
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

