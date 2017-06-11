package edu.wright.cs.dase.usgs;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import com.google.gson.Gson;

import static spark.Spark.*;

import org.aksw.owl2sparql.OWLClassExpressionToSPARQLConverter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.io.StringDocumentSource;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class Main {
	
	private static HashMap<String, OWLOntology> ontologies = new HashMap<>();

	// Must have coordinate data
	private static String baseOntology;

	// A dataset URL on a SPARQL server
	private static String sparqlEndpoint;

	private static HashMap<String, ArrayList<Similarity>> relations;
	private static String ont1Relations = "";
	private static String ont2Relations = "";
	private static double synWeight = 0.0;
	private static double semWeight = 0.0;
	private static double structWeight = 0.0;
	
	public static void main(String[] args) {
		Properties config = new Properties();
		try (InputStream inputStream = new FileInputStream("config.properties")) {
			if (inputStream != null) {
				config.load(inputStream);
			} else {
				throw new FileNotFoundException("property file 'config.properties' not found");
			}
		} catch (IOException e) {
			System.out.println("Error reading config file");
			System.exit(1);
		}

		baseOntology = config.getProperty("baseOntology");
		sparqlEndpoint = config.getProperty("sparqlDataset");

		if (sparqlEndpoint.charAt(sparqlEndpoint.length() - 1) != '/') {
			sparqlEndpoint += '/';
		}
		sparqlEndpoint += "query";

		Properties local = new Properties();
		try (InputStream inputStream = new FileInputStream("local.properties")) {
			if (inputStream != null) {
				local.load(inputStream);
				for (Enumeration<?> e = local.propertyNames(); e.hasMoreElements();) {
					String key = (String)e.nextElement();
					config.setProperty(key, local.getProperty(key));
				}
			} else {
				// not present, never mind
			}
		} catch (IOException e) {
			// ignore
		}

//		ArrayList<Ontology> ontologies = getOntologies();
//		for (Ontology ont: ontologies) {
//			System.out.println(ont);
//		}
		
//		ArrayList<Entity> entities = getEntities("USGS.owl");
//		for (Entity e: entities) {
//			System.out.println(e);
//		}
		
//		ArrayList<Axiom> axioms = getAxioms(
//				"http://spatial.maine.edu/semgaz/HydroOntology#Wetlands", 
//				"Hydro3.owl", "USGS.owl");
//		for (Axiom a: axioms) {
//			System.out.println(a.getOWL());
//		}
//		
//		String s = "<EquivalentClasses>"
//				+ "<Class IRI=\"http://spatial.maine.edu/semgaz/HydroOntology#Wetlands\"/>"
//				+ "<Class IRI=\"http://cegis.usgs.gov/SWO/Coastline\"/>"
//				+ "</EquivalentClasses>";
//		s = "EquivalentClasses ("
//				+ "\"http://spatial.maine.edu/semgaz/HydroOntology#Wetlands\" "
//				+ "\"http://cegis.usgs.gov/SWO/Coastline\" "
//				+ ")";
//		addAxiom(s, "Hydro3.owl", "USGS.owl");
//		
//		axioms = getAxioms(
//				"http://spatial.maine.edu/semgaz/HydroOntology#Wetlands", 
//				"Hydro3.owl", "USGS.owl");
//		for (Axiom a: axioms) {
//			System.out.println(a.getOWL());
//		}
		
//		removeAxiom(s, "Hydro3.owl", "USGS.owl");
//		
//		axioms = getAxioms(
//				"http://spatial.maine.edu/semgaz/HydroOntology#Wetlands", 
//				"Hydro3.owl", "USGS.owl");
//		for (Axiom a: axioms) {
//			System.out.println(a);
//		}
		
		
//		axioms = getAxioms(
//				"http://spatial.maine.edu/semgaz/HydroOntology#Wetlands", 
//				"Hydro3.owl", "USGS.owl");
//		for (Axiom a: axioms) {
//			System.out.println(a);
//		}
		
//		String s = "SubClassOf(<http://spatial.maine.edu/semgaz/HydroOntology#Watershed> "
//				+ "ObjectUnionOf(<http://cegis.usgs.gov/SWO/LakeOrPond> <http://cegis.usgs.gov/SWO/SwampOrMarsh>))";
//		s = "<SubClassOf>"
//				+ "<Class IRI=\"http://spatial.maine.edu/semgaz/HydroOntology#Watershed\"/>"
//				+ "<ObjectUnionOf>"
//				+ "<Class IRI=\"http://cegis.usgs.gov/SWO/LakeOrPond\"/>"
//				+ "<Class IRI=\"http://cegis.usgs.gov/SWO/SwampOrMarsh\"/>"
//				+ "</ObjectUnionOf>"
//				+ "</SubClassOf>";
//		HashMap<Entity, ArrayList<Coordinates>> coords = getCoordinates(s, 
//				"Hydro3.owl", "USGS.owl", 38.99237332729, -82.3558901826);
//		for (Entity e: coords.keySet()) {
//			System.out.println(e);
//			for (Coordinates c: coords.get(e)) {
//				System.out.println("\t" + c);
//			}
//		}

//		ArrayList<ClassEntity> entities = getClasses("Hydro3.owl");
//		for (Entity e: entities) {
//			System.out.println(e);
//			
//			ArrayList<Entity> relatedEntities = getRelatedClasses(e.getURI(), "Hydro3.owl", "USGS.owl", .2, .5, .3);
//			for (int i=0; i<3; i++) {
//				System.out.println("\t" + relatedEntities.get(i));
//			}
//		}

		if (args.length > 0 && args[0].equals("serve")) {
			Gson gson = new Gson();

			port(8080);
			staticFiles.location("/public");

			get("/config", (reqest, response) -> {
				return config;
			}, gson::toJson);
		
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
				if (cls != null) {
					return getAxioms(cls, ont1, ont2);
				} else {
					String axiom = request.queryParams("axiom");
					ArrayList<Axiom> list = new ArrayList<>();
					list.add(getAxiom(axiom, ont1, ont2));
					return list;
				}
			}, gson::toJson);

			post("/axioms", (request, response) -> {
				String axiom = request.body();
				String ont1 = request.queryParams("ontology1");
				String ont2 = request.queryParams("ontology2");
				return addAxiom(axiom, ont1, ont2).getOWL();
			});

			delete("/axioms", (request, response) -> {
				String axiom = request.body();
				String ont1 = request.queryParams("ontology1");
				String ont2 = request.queryParams("ontology2");
				removeAxiom(axiom, ont1, ont2);
				return "OK";
			});

			get("/coordinates", (request, response) -> {
				String axiom = request.queryParams("axiom");
				String ont1 = request.queryParams("ontology1");
				String ont2 = request.queryParams("ontology2");
				Double lat = new Double(request.queryParams("lat"));
				Double lng = new Double(request.queryParams("lng"));
				return getCoordinates(axiom, ont1, ont2, lat, lng);
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
		
		File ontDir = new File("data/ontologies/");
		
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
			if (ax.toString().contains(entityURI) && Axiom.isInteresting(ax)) {
				axioms.add(new Axiom(ax, alignmentOnt, ont1Filename, ont2Filename, ont2));
			}
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
			if (Axiom.getOWL(ax, alignmentOnt).equals(axiomOWL)) {
				return new Axiom(ax, alignmentOnt, ont1Filename, ont2Filename, ont2);
			}
		}
		
		return null;
	}
	
	
	// add the new axiom to the alignment file, refresh the alignment
	public static Axiom addAxiom(String axiomOWL, String ont1Filename, String ont2Filename) {
		
		// get the alignment ontology
		String alignmentFilename = ont1Filename.replaceAll(".owl", "") + "-" + 
				ont2Filename.replaceAll(".owl", "") + ".owl";
		
		File orig = new File("data/alignments/" + alignmentFilename);
		
		OWLOntology alignmentOnt = getOntology(alignmentFilename, true);
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		
		// create an ontology that is just this axiom
		StringDocumentSource in = new StringDocumentSource("<Ontology>" + axiomOWL + "</Ontology>");
		try {
			OWLOntology axiomOnt = manager.loadOntologyFromOntologyDocument(in);

			// add the axiom into the alignmentOnt
			Set<OWLAxiom> temp = axiomOnt.getAxioms();
			if (temp == null || temp.size() == 0) {
				return null;
			}
			
			OWLAxiom axArg = temp.iterator().next();
			manager.applyChange(new AddAxiom(alignmentOnt, axArg));

			// write out the alignment ontology to its file
			manager.saveOntology(alignmentOnt, new OWLXMLDocumentFormat(), IRI.create(orig));
			
			OWLOntology ont2 = getOntology(ont2Filename, false);
			return new Axiom(axArg, alignmentOnt, ont1Filename, ont2Filename, ont2);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	// remove the new axiom from the alignment ontology and update the corresponding alignment file
	public static void removeAxiom(String axiomOWL, String ont1Filename, String ont2Filename) {
		
		// get the alignment ontology
		String alignmentFilename = ont1Filename.replaceAll(".owl", "") + "-" + 
				ont2Filename.replaceAll(".owl", "") + ".owl";
		
		File orig = new File("data/alignments/" + alignmentFilename);
		
		OWLOntology alignmentOnt = getOntology(alignmentFilename, true);
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		
		// create an ontology that is just this axiom
		StringDocumentSource in = new StringDocumentSource("<Ontology>" + axiomOWL + "</Ontology>");
		try {

			OWLOntology axiomOnt = manager.loadOntologyFromOntologyDocument(in);

			// remove the axiom from the alignmentOnt
			Set<OWLAxiom> temp = axiomOnt.getAxioms();
			if (temp == null || temp.size() == 0) return;
			
			OWLAxiom axArg = temp.iterator().next();
			OWLAxiom toRemove = null;
			for (OWLAxiom ax: alignmentOnt.getAxioms()) {
				if (axArg.toString().equals(ax.toString())) {
					toRemove = ax;
					break;
				}
			}
		
			if (toRemove == null) return;
			manager.applyChange(new RemoveAxiom(alignmentOnt, toRemove));

			// write out the alignment ontology to its file
			manager.saveOntology(alignmentOnt, new OWLXMLDocumentFormat(), IRI.create(orig));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public static ArrayList<Similarity> getRelatedClasses(String entityURI, String ont1, String ont2, 
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
			String ont1Filename, String ont2Filename, double lat, double lng) {
		
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
				
				String spatialStuff = ""
					+ "?geom <http://jena.apache.org/spatial#nearby> (" 
					+ lat + " " + lng + " <RANGE> 'km') . \n"
					+ "?geom <http://cegis.usgs.gov/SWO/isGeometryOf> ?x . \n"
					+ "?geom <http://www.opengis.net/ont/geosparql#asWKT> ?shape . \n";

				OWLClassExpression ce = ((ClassEntity) ent).getEntity();
				Query q = converter.asQuery(ce, "?x");

				String classStuff = q.toString();
				int beginIndex = classStuff.indexOf("{")+1;
				int endIndex = classStuff.lastIndexOf("}");
				classStuff = classStuff.substring(beginIndex, endIndex).trim();
				
				
				// perform count queries first, to set the search radius appropriately
				String countQuery = "PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
						+ "SELECT (COUNT(?shape) AS ?count) WHERE {\n"
						+ spatialStuff + classStuff + "}";
				
				int range = 1;
				int count = 0;
				int attempts = 0;
				int step = 4;
				
				while (count == 0 && ++attempts < 5) {
					String filledQuery = countQuery.replaceAll("<RANGE>", "" + range);
					count = getCountInRange(filledQuery);
					range *= step;
				}
				range /= step; // go back one level 
				
				query = "PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
						+ "SELECT ?shape WHERE {\n"
						+ spatialStuff + classStuff + "}";
				
				String filledQuery = query.replaceAll("<RANGE>", "" + range);		
				coordinates = getCoordinates(filledQuery);
				entityCoordinates.put(ent, coordinates);
			}
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
	
	
	private static int getCountInRange(String query) {
        
        QueryExecution qe = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);

        ResultSet results = qe.execSelect();
        QuerySolution result = results.next();
        String countString = result.getLiteral("?count").toString();
        countString = countString.substring(0, countString.indexOf("^"));
        int count = Integer.parseInt(countString);
        qe.close();
        
        return count;
	}
	
	
	public static OWLOntology getOntology(String ontFilename, boolean isAlignment) {
		
		OWLOntology ont = null;
		
		String path = "data/ontologies/";
		if (isAlignment) {
			path = "data/alignments/";
		}
		
		ontFilename = ontFilename.trim();
		
		if (ontologies.containsKey(ontFilename)) {
			ont = ontologies.get(ontFilename);
			
		} else {
			
			try {
				OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
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

