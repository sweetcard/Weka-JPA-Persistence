package org.weka.jpa;

import java.io.File;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager; 
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.Temporal;

import org.slf4j.Logger;


import weka.core.Attribute; 
import weka.core.Instances;
import weka.core.converters.ArffSaver;

public class Weka2JPAHelper {
	
	@Inject
	private Logger log;
	
	@Inject  
	@Named("SocialSLA")
	private EntityManager em;
	

	public <E> void save(File p_file, Collection<E> p_list, Class<E> p_class) {
		// TODO Create a function to save data from list of Entities
		
	}

	public <E> void save(File p_file, Class<E> p_entityClass) throws IOException {

		Instances l_data = createInstance(p_entityClass);

		ArffSaver saver = new ArffSaver();
		saver.setInstances(l_data);
		saver.setFile(p_file);
		saver.writeBatch();
	}

	private <E> Instances createInstance(Class<E> p_entityClass) {

		if (!p_entityClass.isAnnotationPresent(Entity.class)) {
			throw new NotEntityWEKAJPARuntimeException();
		}

		Field[] l_fields = p_entityClass.getDeclaredFields();

		ArrayList<Attribute> l_atts = new ArrayList<Attribute>(l_fields.length);

		Map<Attribute, Field> l_mapAttributeToField = new HashMap<>(l_fields.length);
		Map<Attribute, Class> l_mapAttributeToClass = new HashMap<>(l_fields.length);
		for (Field l_field : l_fields) {
			Attribute l_att = null;
			Annotation l_annot;
			if ((l_annot = l_field.getDeclaredAnnotation(ManyToMany.class)) != null) {
				l_att = createAttributeFromManyToMany(l_field);
			}else if ((l_annot = l_field.getDeclaredAnnotation(ManyToOne.class)) != null) {

				ArrayList<String> l_refValues = new ArrayList<>();
				String l_qlString = "SELECT E FROM " + l_field.getType().getSimpleName() + " E ";
				Query l_query = em.createQuery(l_qlString);
				List l_list = l_query.getResultList();
				for (Object l_object : l_list) {
					l_refValues.add(l_object.toString());
				}
				// attVals.add(POSITIVO);
				// attVals.add(NEUTRO);
				// attVals.add(NEGATIVO);
				// atts.add(new Attribute("Sentiment", attVals));
				
				// TODO: based on the field, to obtain the persistence layer,
				// possible reference values.
				// TODO: how to make low coupling with the persistence of the
				// original system? Should be injected one PersistenceEntity,
				// regardless of its implementation.
				l_att = createAttributeFromManyToOne(l_field, l_refValues);
			} else if ((l_annot = l_field.getDeclaredAnnotation(OneToMany.class)) != null) {
				l_att = createAttributeFromOneToMany(l_field);
			} else if ((l_annot = l_field.getDeclaredAnnotation(OneToOne.class)) != null) {
				l_att = createAttributeFromOneToOne(l_field);
			} else if ((l_annot = l_field.getDeclaredAnnotation(Temporal.class)) != null) {
				l_att = createAttributeFromTemporal(l_field);
			} else if ((l_annot = l_field.getDeclaredAnnotation(Column.class)) != null) {
				l_att = createAttributeFromColumn(l_field);
			} else {
				// qualquer outra anotação será ignorada
				continue;
			}
			l_atts.add(l_att);
			l_mapAttributeToClass.put(l_att, l_field.getType());
			l_mapAttributeToField.put(l_att, l_field);
		}
		// 1. set up attributes


		// 2. create Instances object
		Instances l_data = new Instances(p_entityClass.getSimpleName(), l_atts,0);

		populateInstanceWithData(l_data, p_entityClass);

		return l_data;
	}

	private <E> void populateInstanceWithData(Instances p_data,Class<E> p_entityClass) {
		// TODO Auto-generated method stub

		// 3. fill with data
		// first instance
		// vals = new double[data.numAttributes()];
		// //vals[0] = data.attribute(0).addStringValue("Texto do Post");
		// vals[1] = data.attribute(1).addStringValue("Contexto do Post");
		// vals[2] = data.attribute(2).addStringValue("Evento ocorrido no
		// Post");
		// vals[3] = attVals.indexOf(POSITIVO);

		// add
		// data.add(new DenseInstance(1.0, vals));

		// vals = new double[data.numAttributes()];
		// vals[0] = data.attribute(0).addStringValue("Texto do Post 2");
		// vals[1] = data.attribute(1).addStringValue("Contexto do Post 2");
		// vals[2] = data.attribute(2).addStringValue("Evento ocorrido no Post
		// 2");
		// vals[3] = attVals.indexOf(NEUTRO);
		// add
		// data.add(new DenseInstance(1.0, vals));
	}

	private Attribute createAttributeFromTemporal(Field p_field) {
		// TODO Auto-generated method stub
		return null;
	}

	private Attribute createAttributeFromOneToOne(Field p_field) {
		// TODO Auto-generated method stub
		return null;
	}

	private Attribute createAttributeFromOneToMany(Field p_field) {
		// TODO Auto-generated method stub
		return null;
	}

	private Attribute createAttributeFromManyToOne(Field p_field, ArrayList<String> p_refListValue) {
		// TODO Auto-generated method stub
		String l_name = createName(p_field);

		Attribute l_att = new Attribute(l_name, p_refListValue);

		return l_att;
	}

	private String createName(Field p_field) {
		Column l_annot = p_field.getDeclaredAnnotation(Column.class);
		String l_name = null;
		if (l_annot != null)
			l_name = l_annot.name();
		if (l_name == null || l_name.isEmpty())
			l_name = p_field.getName();

		return l_name;
	}

	private Attribute createAttributeFromManyToMany(Field p_field) {

		String l_name = createName(p_field);

		Attribute l_att = null;
		if (p_field.getType() == String.class) {
			l_att = new Attribute(l_name, (ArrayList<String>) null);
		} else if (p_field.getType().isAssignableFrom(Number.class)) {
			l_att = new Attribute(l_name);
		}
		return l_att;
	}

	private Attribute createAttributeFromColumn(Field p_field) {
		// TODO Auto-generated method stub
		String l_name = createName(p_field);
		Attribute l_att = null;
		if (p_field.getType() == String.class) {
			l_att = new Attribute(l_name, (ArrayList<String>) null);
		} else if (p_field.getType().isAssignableFrom(Number.class)) {
			l_att = new Attribute(l_name);
		}
		return l_att;
	}


}
