package edu.wright.cs.dase.usgs;

import java.io.Serializable;

import org.semanticweb.owlapi.model.OWLClassExpression;

public class ClassEntity extends Entity implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private OWLClassExpression entity;
	
	public ClassEntity(String ontology, OWLClassExpression ent) {
		super(ontology, ent.toString(), Entity.getEntityLabel(ent));
		this.entity = ent;
	}
	
	public OWLClassExpression getEntity() {
		return entity;
	}
}
