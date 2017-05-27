package edu.wright.cs.dase.usgs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.parameters.Imports;

import com.wcohen.ss.ScaledLevenstein;


public class AutomatedAlignment {
	
	private static ScaledLevenstein lev = new ScaledLevenstein();
	private static HashMap<String, String> wikipediaResults = new HashMap<>();
	
	
	@SuppressWarnings("unchecked")
	public static HashMap<String, ArrayList<Entity>> getSimilarities(
			String ont1, String ont2, double syn, double sem, double struct) {
		
		// if the file doesn't exist, then proceed with the steps below
		HashMap<String, ArrayList<Entity>> simMap = new HashMap<>();
		
		// read in the Wikipedia cache to speed up the semantic similarity computation
		File file = new File("wikipedia.dat");
		if (file.exists()) {
			try {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
				wikipediaResults = (HashMap<String, String>) ois.readObject();
				ois.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		ArrayList<ClassEntity> ont1Entities = Main.getClasses(ont1);
		ArrayList<ClassEntity> ont2Entities = Main.getClasses(ont2);
		
		for (ClassEntity e1: ont1Entities) {		
			
			if (e1.getEntity().isAnonymous()) continue;
			
			ArrayList<Similarity> similarities = new ArrayList<>();

			for (ClassEntity e2: ont2Entities) {	
				similarities.add(new Similarity(e2, computeSimilarity(e1, e2, syn, sem, struct)));
			}
			
			Collections.sort(similarities);

			ArrayList<Entity> orderedEntities = new ArrayList<>();
			for (Similarity sim: similarities) {
				orderedEntities.add(sim.getEntity());
			}
			
			simMap.put(e1.getURI(), orderedEntities);
		}
		
		// write the Wikipedia cache to a file
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File("wikipedia.dat")));
			oos.writeObject(wikipediaResults);
			oos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return simMap;
	}
	
	
	public static double computeSimilarity(ClassEntity ent1, ClassEntity ent2, 
			double synWeight, double semWeight, double structWeight) {
		
		double syntacticSim = getSyntacticSimilarity(ent1, ent2);
		double semanticSim = getSemanticSimilarity(ent1, ent2);
		double structuralSim = getStructuralSimilarity(ent1, ent2);
		
		return (synWeight * syntacticSim + semWeight * semanticSim + structWeight * structuralSim);
	}
	
	
	public static double getSyntacticSimilarity(Entity ent1, Entity ent2) {
		double levSim = lev.score(ent1.getLabel(), ent2.getLabel());
		double lcsSim = LongestCommonSubstring.lcsSim(ent1.getLabel(), ent2.getLabel());
		return Math.max(levSim, lcsSim);
	}
	
	
	public static double getSyntacticSimilarity(String s1, String s2) {
		double levSim = lev.score(s1, s2);
		double lcsSim = LongestCommonSubstring.lcsSim(s1, s2);
		return Math.max(levSim, lcsSim);
	}
	
	
	public static double getSemanticSimilarity(Entity ent1, Entity ent2) {
		
		String label1 = ent1.getLabel();
		String label2 = ent2.getLabel();
		
		// first check the cache to avoid querying Wikipedia unnecessarily
		String wiki1 = wikipediaResults.get(label1);
		String wiki2 = wikipediaResults.get(label2);
				
		// if this wasn't in the cache, query Wikipedia (with redirection) for the entity labels
		if (wiki1 == null) {
			wiki1 = WikipediaOperations.getClosestArticle(label1);
			wikipediaResults.put(label1, wiki1);
		}
		
		if (wiki2 == null) {
			wiki2 = WikipediaOperations.getClosestArticle(label2);
			wikipediaResults.put(label2, wiki2);
		}
		
		// if the results are identical, the entities are perfectly similar
		if (wiki1.equals(wiki2)) return 1.0;
		
		// otherwise return the greatest overlap between one entity's label
		// and the other entity's Wikipedia results
		return 0.75 * Math.max(overlap(wiki1, label2), overlap(wiki2, label1));
	}
	
	
	public static double getStructuralSimilarity(ClassEntity ent1, ClassEntity ent2) {
		
		if (ent1.getEntity().isAnonymous()) return 0.0;
		if (ent2.getEntity().isAnonymous()) return 0.0;
		
		OWLOntology ont1 = Main.getOntology(ent1.getOntology(), false);
		OWLOntology ont2 = Main.getOntology(ent2.getOntology(), false);
		
		Set<OWLAxiom> axioms1 = ont1.getAxioms(Imports.EXCLUDED);
		Set<OWLAxiom> axioms2 = ont2.getAxioms(Imports.EXCLUDED);
		
		Set<OWLNamedObject> set1 = getRelatedEntities(ent1.getEntity(), axioms1);
		Set<OWLNamedObject> set2 = getRelatedEntities(ent2.getEntity(), axioms2);
		
		return setSim(set1, set2);
	}
	
	
	private static Set<OWLNamedObject> getRelatedEntities(OWLClassExpression ent, Set<OWLAxiom> axioms) {
		Set<OWLNamedObject> related = new HashSet<>();
		
		for (OWLAxiom ax: axioms) {
			
			if (!ax.toString().contains(ent.toString())) continue;

			String axType = ax.getAxiomType().toString();
			
			if (axType.contains("Class") || axType.contains("Property")) {
				
				Set<OWLClass> classes = ax.getClassesInSignature();
				
				for (OWLClass cls: classes) {
					if (cls.isAnonymous()) continue;
					if (cls.toString().equals(ent)) continue;
					related.add((OWLNamedObject) cls);
				}
				
				Set<OWLNamedIndividual> inds = ax.getIndividualsInSignature();
				
				for (OWLNamedIndividual ind: inds) {
					if (ind.isAnonymous()) continue;
					if (ind.toString().equals(ent)) continue;
					related.add((OWLNamedObject) ind);
				}
				
				Set<OWLDataProperty> dProps = ax.getDataPropertiesInSignature();
				
				for (OWLDataProperty prop: dProps) {
					if (prop.isAnonymous()) continue;
					related.add((OWLNamedObject) prop);
				}
				
				Set<OWLObjectProperty> oProps = ax.getObjectPropertiesInSignature();
				
				for (OWLObjectProperty prop: oProps) {
					if (prop.isAnonymous()) continue;
					related.add((OWLNamedObject) prop);
				}
			}
		}
		
		return related;
	}
	
	
	private static double setSim(Set<OWLNamedObject> set1, Set<OWLNamedObject> set2) {
		double sum = 0.0;
		
		HashSet<String> labelSet1 = new HashSet<>();
		HashSet<String> labelSet2 = new HashSet<>();
		
		for (OWLNamedObject s1: set1)
			labelSet1.add(Entity.getEntityLabel(s1));
			
		for (OWLNamedObject s2: set2)
			labelSet2.add(Entity.getEntityLabel(s2));
			
		for (String label1: labelSet1) {
			double best = 0.0;
			for (String label2: labelSet2) {
				double current = getSyntacticSimilarity(label1, label2);
				if (current > best) best = current;
			}
			sum += best;
		}
		
		if (labelSet1.size() == 0) return 0.0;
		return sum / labelSet1.size();
	}

	
	private static double overlap(String s, String label) {
		
		int count = 0;
		int overlap = 0;
		
		String[] tokens = label.split("[ ]");
		for (String token: tokens) {
			if (Stopwords.isStopword(s)) continue;
			count++;
			if (s.contains(token)) {
				overlap++;
			}
		}
		
		return count == 0 ? 0.0 : overlap / (double) count;
	}
}
