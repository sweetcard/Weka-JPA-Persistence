package org.weka.jpa;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
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
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

public class Weka2JPAHelper {

	@Inject
	private Logger log;
	
	@Inject  
	@Named("WekaPersistence")
	private EntityManager em;

	public <E> void save(File p_file, Class<E> p_entityClass, Collection<E> p_list) throws IOException {
		Instances l_data = createInstance(p_entityClass, p_list);

		ArffSaver saver = new ArffSaver();
		saver.setInstances(l_data);
		saver.setFile(p_file);

		saver.writeBatch();
	}

	public <E> void save(File p_file, Class<E> p_entityClass) throws IOException {

		Instances l_data = createInstance(p_entityClass, null);

		ArffSaver saver = new ArffSaver();
		saver.setInstances(l_data);
		saver.setFile(p_file);

		saver.writeBatch();
	}

	private <E> Instances createInstance(Class<E> p_entityClass, Collection<E> p_list) {

		if (!p_entityClass.isAnnotationPresent(Entity.class)) {
			throw new NotEntityWEKAJPARuntimeException();
		}

		Field[] l_fields = p_entityClass.getDeclaredFields();

		ArrayList<Attribute> l_atts = new ArrayList<Attribute>(l_fields.length);

		Map<Attribute, Field> l_mapAttributeToField = new HashMap<>(l_fields.length);
		Map<Attribute, Class> l_mapAttributeToClass = new HashMap<>(l_fields.length);
		Map<Attribute, ArrayList<String>> l_mapAttributeToRefVAlues = new HashMap<>(l_fields.length);
		for (Field l_field : l_fields) {
			Attribute l_att = null;
			Annotation l_annot;
			Class<?> l_type = l_field.getType();
			if ((l_annot = l_field.getDeclaredAnnotation(ManyToMany.class)) != null) {
				l_att = createAttributeFromManyToMany(l_field);
			} else if ((l_annot = l_field.getDeclaredAnnotation(ManyToOne.class)) != null) {

				ArrayList<String> l_refValues = new ArrayList<>();

				String l_qlString = "SELECT E FROM " + l_type.getSimpleName() + " E ";
				Query l_query = em.createQuery(l_qlString);
				List<E> l_list = l_query.getResultList();

				for (Object l_object : l_list) {
					// TODO: how to identify the best way to convert the child
					// entity in a string or number to be referenced, in
					// addition you need to worry about the correct order of
					// obtaining or using an index synchronized with the
					// database.
					// TODO: An alternative would be the annotation specifies
					// Weka to indicate a method for obtaining a string or
					// double / float representing the object. Remember that
					// this field should also be indexed and stored in the
					// database, Commission is therefore proposing should be
					// noted as a Column type, Temporal, Id, etc.
					l_refValues.add(l_object.toString());
				}
				l_att = createAttributeFromManyToOne(l_field, l_refValues);
				l_mapAttributeToRefVAlues.put(l_att, l_refValues);
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
			l_mapAttributeToClass.put(l_att, l_type);
			l_mapAttributeToField.put(l_att, l_field);
		}
		// 1. set up attributes

		Instances l_data = populateInstanceWithData(l_mapAttributeToRefVAlues, l_mapAttributeToField, l_atts,
				p_entityClass, p_list);

		return l_data;
	}

	private <E> Instances populateInstanceWithData(Map<Attribute, ArrayList<String>> p_mapAttributeToRefValues,
			Map<Attribute, Field> p_mapAttributeToField, ArrayList<Attribute> p_atts, Class<E> p_entityClass,
			Collection<E> p_list) {
		// TODO Auto-generated method stub

		// 2. create Instances object
		Instances l_data = new Instances(p_entityClass.getSimpleName(), p_atts, 0);

		List<E> l_list;
		if (p_list == null) {
			String l_qlString = "SELECT E FROM " + p_entityClass.getSimpleName() + " E ";

			Query l_query = em.createQuery(l_qlString);

			l_list = l_query.getResultList();
		} else
			l_list = new ArrayList<>(p_list);

		for (final E l_object : l_list) {
			final double[] l_vals = new double[l_data.numAttributes()];

			p_mapAttributeToField.forEach((p_att, p_field) -> {
				// To facilitate debugging,
				// Furthermore the Eclipse was not seeing the parameter p_att;
				Attribute l_att = p_att;// to facilitate debugging
				Field l_field = p_field;// to facilitate debugging
				@SuppressWarnings("unchecked")
				E l_obj = (E) l_object;// to facilitate debugging
				ArrayList<Attribute> l_atts = p_atts;// to facilitate debugging

				try {

					int l_attIndex = l_atts.indexOf(l_att);
					Class<?> l_fieldType = l_field.getType();
					l_field.setAccessible(true);

					double l_val = -1;
					if (l_fieldType == String.class) {
						String l_string = (String) l_field.get(l_obj);
						if (l_string != null)
							l_val = l_data.attribute(l_attIndex).addStringValue(l_string);

					} else if (l_fieldType.isAssignableFrom(Number.class)) {
						Double l_double = l_field.getDouble(l_obj);
						if (l_double != null)
							;
						l_val = l_double;
					} else {
						ArrayList<String> l_refValues = p_mapAttributeToRefValues.get(l_att);
						Object l_value = l_field.get(l_obj);
						if (l_value != null) {
							String l_string = l_value.toString();
							l_val = l_refValues.indexOf(l_string);
						}
					}
					l_vals[l_attIndex] = l_val;
				} catch (Exception e) {
					// does nothing ignores the value?
					log.info(e.getMessage());
				}

			});
			DenseInstance l_instance = new DenseInstance(1.0, l_vals);
			for (int l_idx = 0; l_idx < l_vals.length; l_idx++) {
				if (l_vals[l_idx] < 0)
					l_instance.setMissing(l_idx);
			}
			l_data.add(l_instance);

		}

		return l_data;
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
