package edu.wright.cs.dase.usgs;

import java.io.Serializable;

import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

public class PropertyEntity extends Entity implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private OWLObjectPropertyExpression entity;
	
	public PropertyEntity(String ontology, OWLObjectPropertyExpression ent) {
		super(ontology, ent.toString(), Entity.getEntityLabel(ent));
		this.entity = ent;
	}
	
	public OWLObjectPropertyExpression getEntity() {
		return entity;
	}
}
