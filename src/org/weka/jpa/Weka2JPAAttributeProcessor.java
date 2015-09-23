package org.weka.jpa;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.Temporal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weka.jpa.utils.CallbackField;
import org.weka.jpa.utils.CallbackFieldToNumber;
import org.weka.jpa.utils.CallbackFieldToString;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class Weka2JPAAttributeProcessor<E> {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final Weka2JPAHelper<E> helper;
	private final String relationBaseName;
	private final Class<E> entityClass;
	private final Field[] fields;
	private final HashMap<Attribute, String> mapAttributeToExtraField;
	private final HashMap<Attribute, List<String>> mapAttributeToRefValues;
	private final HashMap<Attribute, Field> mapAttributeToField;

	public Weka2JPAAttributeProcessor(Class<E> p_entityClass, Weka2JPAHelper<E> p_helper) {

		helper = p_helper;

		// log.info("Populando Instancias com relação aos atributos: " +
		// p_atts);
		relationBaseName = p_entityClass.getSimpleName();
		log.info("Instancias Relacionadas a " + relationBaseName);

		entityClass = p_entityClass;

		fields = p_entityClass.getDeclaredFields();
		log.info("Campos Declarados: " + fields);

		mapAttributeToExtraField = new HashMap<>(getExtraFieldsNames().size());
		mapAttributeToRefValues = new HashMap<>(fields.length);
		mapAttributeToField = new HashMap<>(fields.length);

	}

	/**
	 * Verificar se o objeto obtido no campo deve ser tornar este campo como um
	 * incognito
	 *
	 * @param p_fieldName
	 * @param l_string
	 * @return
	 */
	private boolean checkMissingFieldValue(String p_fieldName, Object l_string) {
		final Object l_value = helper.mapMissingValueToFields.get(p_fieldName);
		if (l_value == null) {
			return false;
		}
		return l_value.equals(l_string);
	}

	private Attribute createAttributeFromColumn(Field p_field) {
		// TODO Auto-generated method stub
		final String l_name = createNameAttribute(p_field);
		Attribute l_att = null;
		if (p_field.getType() == String.class) {
			l_att = new Attribute(l_name, (ArrayList<String>) null);
		} else if (p_field.getType().isAssignableFrom(Number.class)) {
			l_att = new Attribute(l_name);
		}
		return l_att;
	}

	private Attribute createAttributeFromManyToMany(Field p_field) {

		final String l_name = createNameAttribute(p_field);

		Attribute l_att = null;
		if (p_field.getType() == String.class) {
			l_att = new Attribute(l_name, (ArrayList<String>) null);
		} else if (p_field.getType().isAssignableFrom(Number.class)) {
			l_att = new Attribute(l_name);
		}
		return l_att;
	}

	/**
	 * Cria um atributo com base em um campo da Entidade de persistencia que
	 * seja do tipo ManyToOne.
	 *
	 * Este método consulta no banco de dados para encontrar as entidades
	 * relacionadas.
	 *
	 * TODO: permitir que seja criado um banco de entidades relacionadas para
	 * ser usada no lugar da consulta no banco.
	 *
	 * @param p_field
	 * @return
	 */
	private Attribute createAttributeFromManyToOne(Field p_field) {

		@SuppressWarnings("rawtypes")
		final ArrayList l_refValues = new ArrayList();

		final String l_qlString = "SELECT E FROM " + p_field.getType().getSimpleName() + " E ";
		final Query l_query = helper.em.createQuery(l_qlString);
		@SuppressWarnings("unchecked")
		final List<E> l_list = l_query.getResultList();
		final String l_fieldName = p_field.getName();

		final CallbackField<?> l_callback = getCallbackClassField(p_field);

		for (final Object l_entityRef : l_list) {

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
			if (l_callback != null) {

				final String l_str = (String) l_callback.call(null, l_fieldName, l_entityRef);
				l_refValues.add(l_str);
			} else {
				final String l_str = l_entityRef.toString();
				l_refValues.add(l_str);
			}
		}
		final Attribute l_att = createAttributeFromManyToOne(p_field, l_refValues);

		return l_att;
	}

	private Attribute createAttributeFromManyToOne(Field p_field, List<String> p_refListValue) {
		final String l_name = createNameAttribute(p_field);

		final Attribute l_att = new Attribute(l_name, p_refListValue);
		mapAttributeToRefValues.put(l_att, p_refListValue);
		return l_att;
	}

	private Attribute createAttributeFromOneToMany(Field p_field) {
		// TODO Auto-generated method stub
		return null;
	}

	private Attribute createAttributeFromOneToOne(Field p_field) {
		// TODO Auto-generated method stub
		return null;
	}

	private Attribute createAttributeFromTemporal(Field p_field) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Cria os atributos usados no cabeçalho e seus metadados para aferição dos
	 * tipos.
	 *
	 * @param p_fields
	 * @param p_mapAttributeToRefVAlues
	 * @param p_mapAttributeToField
	 * @return
	 */
	public ArrayList<Attribute> createAttributes() {

		final ArrayList<Attribute> l_atts = new ArrayList<Attribute>(fields.length);

		for (final Field l_field : fields) {
			Attribute l_att = null;
			final Class<?> l_typeField = l_field.getType();
			final String l_nameField = l_field.getName();
			if (helper.ignoreFieldsTypeOf.contains(l_typeField) || helper.ignoreFieldsName.contains(l_nameField)
					|| l_nameField.equals("serialVersionUID")) {
				continue;
			}

			if (l_field.getDeclaredAnnotation(Column.class) != null) {
				l_att = createAttributeFromColumn(l_field);
			} else if (l_field.getDeclaredAnnotation(ManyToOne.class) != null) {
				l_att = createAttributeFromManyToOne(l_field);
			} else if (l_field.getDeclaredAnnotation(OneToMany.class) != null) {
				l_att = createAttributeFromOneToMany(l_field);
				continue; // TODO o metodo createAttributeFromOneToMany não está
							// implementado ainda
			} else if (l_field.getDeclaredAnnotation(OneToOne.class) != null) {
				l_att = createAttributeFromOneToOne(l_field);
				continue;// TODO o metodo createAttributeFromOneToOne não está
							// implementado ainda
			} else if (l_field.getDeclaredAnnotation(ManyToMany.class) != null) {
				l_att = createAttributeFromManyToMany(l_field);
				continue;// TODO o metodo createAttributeFromManyToMany não está
							// implementado ainda
			} else if (l_field.getDeclaredAnnotation(Temporal.class) != null) {
				l_att = createAttributeFromTemporal(l_field);
				continue;// TODO o metodo createAttributeFromTemporal não está
							// implementado ainda
			} else {
				// qualquer outra anotação será ignorada
				continue;
			}
			l_atts.add(l_att);
			mapAttributeToField.put(l_att, l_field);
		}

		log.info("Atributos Criados: " + l_atts);
		createAttributesExtraFields(l_atts);

		log.info("Lista Final dos Atributos: " + l_atts);

		return l_atts;
	}

	/**
	 * Cria os atributos relativos aos compos extras adicionados.
	 *
	 * @param p_atts
	 *
	 * @param p_atts
	 */
	private void createAttributesExtraFields(ArrayList<Attribute> p_atts) {

		log.info("Criando Atributos Extras");

		for (final String l_extraField : getExtraFieldsNames()) {
			Attribute l_att = null;

			final Object l_defaultValue = getDefaultValue(l_extraField);
			if (l_defaultValue == null) {
				l_att = new Attribute(l_extraField);
			} else if (l_defaultValue instanceof String) {
				l_att = new Attribute(l_extraField, (ArrayList<String>) null);
			} else if (l_defaultValue.getClass().isAssignableFrom(Number.class)) {
				l_att = new Attribute(l_extraField);
			} else {
				@SuppressWarnings({ "rawtypes" })
				final CallbackField l_callback = getCallBackExtraField(l_extraField);

				if (l_callback instanceof CallbackFieldToNumber) {
					l_att = new Attribute(l_extraField);
				} else {
					l_att = new Attribute(l_extraField, (List<String>) null);
				}
			}
			p_atts.add(l_att);
			mapAttributeToExtraField.put(l_att, l_extraField);
		}
		log.info("Novos Atributos, Lista geral: " + p_atts);
	}

	public Instances createInstances(ArrayList<Attribute> p_atts, Collection<E> l_list) {
		final Instances l_instances = new Instances(getRelationBaseName(), p_atts, 0);
		for (final E l_object : l_list) {
			final double[] l_vals = new double[l_instances.numAttributes()];

			final ArrayList<Attribute> l_incoginitoAttributes = new ArrayList<>();
			processValueFromEachField(l_object, l_instances, l_vals, p_atts, l_incoginitoAttributes);

			processValueFromEachExtraField(l_object, l_instances, l_vals, p_atts, l_incoginitoAttributes);

			// TODO parametrizar o tipo de ARFF:
			// DenseInstance,
			// SparseInstance,
			// BinarySparseInstance
			final DenseInstance l_instance = new DenseInstance(1.0, l_vals);
			for (final Attribute l_att : l_incoginitoAttributes) {
				l_instance.setMissing(l_att.index());
			}
			l_instances.add(l_instance);

		}
		return l_instances;
	}

	/**
	 * Cria o nome do Attributo para uso no arquivo ARFF
	 *
	 * Cria o nome do Atributo com base na anotação Column, caso ela não exista
	 * ou o nome não seja informado utiliza o nome do campo informado.
	 *
	 * @param p_field
	 * @return
	 */
	private String createNameAttribute(Field p_field) {
		final Column l_annot = p_field.getDeclaredAnnotation(Column.class);
		String l_name = null;
		if (l_annot != null) {
			l_name = l_annot.name();
		}
		if (l_name == null || l_name.isEmpty()) {
			l_name = p_field.getName();
		}

		return l_name;
	}

	/**
	 * Obtem o CallBack para um determinado campo de uma entidade com base no
	 * nome do campo
	 *
	 * @param l_fieldName
	 * @return
	 */
	private CallbackField<?> getCallBackClass(String l_fieldName) {

		final CallbackField<?> l_callback = helper.baseClassFieldClassCallBack.get(l_fieldName);
		return l_callback;
	}

	private CallbackField<?> getCallbackClassField(Class<?> p_class) {
		return helper.baseClassFieldClassCallBack.get(p_class);
	}

	/**
	 * Obtem o CallBack para um determinado campo da entidade.
	 *
	 * @param p_field
	 * @return
	 */
	private CallbackField<?> getCallbackClassField(Field p_field) {
		final String l_fieldName = p_field.getName();
		CallbackField<?> l_callback = getCallBackClass(l_fieldName);
		if (l_callback == null) {
			l_callback = getCallbackClassField(p_field.getType());
		}
		return l_callback;
	}

	private CallbackField<?> getCallBackExtraField(String p_fieldName) {

		return helper.baseClassFieldCallBack.get(p_fieldName);
	}

	/**
	 * Retorna a Classe padrão de um determinado campo extra informado.
	 *
	 * @param p_field
	 * @return
	 */
	private Class<?> getDefaultClassExtraField(String p_field) {
		return helper.baseClassDefaultClassExtraField.get(p_field);
	}

	/**
	 * Retorna o valor padrão para um determinado campo.
	 *
	 * @param l_fieldName
	 * @return
	 */
	private Object getDefaultValue(String l_fieldName) {

		return helper.baseClassDefaultValuesExtraField.get(l_fieldName);
	}

	private Set<String> getExtraFieldsNames() {
		return helper.baseClassExtraFieldsNames;
	}

	private List<String> getReferenceValues(Attribute l_att) {
		return mapAttributeToRefValues.get(l_att);
	}

	/**
	 * Nome será usada como referencia para construir o campo \@Relation do
	 * arquivo ARFF
	 *
	 * @return
	 */
	public String getRelationBaseName() {
		return relationBaseName;
	}

	/**
	 * Processa os valores referentes aos compos extras.
	 *
	 * Esta função é altamente dependente dos callbacks para gerar novos
	 * valores.
	 *
	 * @param p_entity
	 * @param p_instances
	 * @param p_listVals
	 * @param p_atts
	 * @param p_incoginitoAttributes
	 */
	private void processValueFromEachExtraField(final E p_entity, final Instances p_instances,
			final double[] p_listVals, final ArrayList<Attribute> p_atts,
			final ArrayList<Attribute> p_incoginitoAttributes) {

		mapAttributeToExtraField.forEach((p_att, p_fieldName) -> {

			try {

				final int l_attIndex = p_atts.indexOf(p_att);

				double l_val = 0;

				final CallbackField<?> l_callBack = getCallBackExtraField(p_fieldName);

				final Object l_defaultValue = getDefaultValue(p_fieldName);

				boolean l_incognito = false;

				if (checkMissingFieldValue(p_fieldName, l_defaultValue)
						|| useNullLikeIncognito() && l_defaultValue == null) {
					l_incognito = true;
				}

				if (l_callBack instanceof CallbackFieldToNumber) {
					@SuppressWarnings("rawtypes")
					final Number l_value = (Number) ((CallbackFieldToNumber) l_callBack).call(p_entity, p_fieldName,
							l_defaultValue);

					if (l_value != null) {
						l_val = l_value.doubleValue();

					} else if (useNullLikeIncognito()) {
						l_incognito = true;
					}

				} else if (l_callBack instanceof CallbackFieldToString) {
					final String l_string = ((CallbackFieldToString) l_callBack).call(p_entity, p_fieldName,
							l_defaultValue);

					if (l_string != null) {
						l_val = p_instances.attribute(l_attIndex).addStringValue(l_string);
					} else if (useNullLikeIncognito()) {
						l_incognito = true;
					}

				} else if (l_defaultValue != null) {
					l_val = p_instances.attribute(l_attIndex).addStringValue(l_defaultValue.toString());
				} else if (useNullLikeIncognito()) {
					l_incognito = true;
				}

				if (l_incognito) {
					p_incoginitoAttributes.add(p_att);
				}

				p_listVals[l_attIndex] = l_val;

			} catch (final Exception e) {
				// does nothing ignores the value?
				log.info(e.getMessage());
			}

		});
	}

	private void processValueFromEachField(final E p_entityObj, final Instances p_instances, final double[] p_vals,
			final ArrayList<Attribute> p_atts, final ArrayList<Attribute> p_incoginitoAttributes) {

		mapAttributeToField.forEach((p_att, p_field) -> {

			try {

				final int l_attIndex = p_atts.indexOf(p_att);

				final Class<?> p_fieldType = p_field.getType();
				p_field.setAccessible(true);

				double l_val = 0;
				boolean l_incognito = false;
				if (p_fieldType == String.class) {
					final String l_string = (String) p_field.get(p_entityObj);

					if (l_string != null) {
						l_val = p_instances.attribute(l_attIndex).addStringValue(l_string);

						if (checkMissingFieldValue(p_field.getName(), l_string)) {
							l_incognito = true;
						}

					} else if (useNullLikeIncognito()) {
						l_incognito = true;
					}

				} else if (p_fieldType.isAssignableFrom(Number.class)) {
					final Double l_double = p_field.getDouble(p_entityObj);

					if (l_double != null) {
						l_val = l_double;

						if (checkMissingFieldValue(p_field.getName(), l_double)) {
							l_incognito = true;
						}

					} else if (useNullLikeIncognito()) {
						l_incognito = true;
					}

				} else {
					final List<String> l_refValues = getReferenceValues(p_att);
					final Object l_value = p_field.get(p_entityObj);

					String l_strValue;
					if (l_value instanceof String) {
						l_strValue = (String) l_value;
					} else {
						final CallbackField<?> l_callback = getCallbackClassField(p_field);

						final String l_fieldName = p_field.getName();
						if (l_callback != null) {
							l_strValue = (String) l_callback.call(p_entityObj, l_fieldName, l_value);
						} else {
							l_strValue = l_value.toString();
						}
					}

					if (useNullLikeIncognito() && l_strValue == null
							|| checkMissingFieldValue(p_field.getName(), l_value)) {
						l_incognito = true;
					}

					l_val = l_refValues.indexOf(l_strValue);
				}
				if (l_incognito) {
					p_incoginitoAttributes.add(p_att);
				}

				p_vals[l_attIndex] = l_val;

			} catch (final Exception e) {
				// does nothing ignores the value?
				log.warn(e.getMessage());
			}
		});
	}

	/**
	 * Retorna se deve ou não usar o valor null como Incognito;
	 *
	 * @return
	 */
	private boolean useNullLikeIncognito() {
		return helper.useNullLikeIncognito;
	}
}
