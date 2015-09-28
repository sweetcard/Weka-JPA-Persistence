package org.weka.jpa;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.Temporal;

import org.apache.commons.lang3.NotImplementedException;
import org.weka.jpa.utils.CallbackField;
import org.weka.jpa.utils.CallbackFieldToNumber;
import org.weka.jpa.utils.ExtraAttributesFromFieldToString;

import weka.core.Attribute;

public class Weka2JPAAttributeProcessor<E> extends Weka2JPAProcessor<E> {

	private final Field[] fields;

	/**
	 *
	 */
	private final HashMap<Attribute, String> mapExtraAttributeFromSlaveField;

	public Weka2JPAAttributeProcessor(Class<E> p_entityClass, Weka2JPAHelper<E> p_helper) {

		super(p_entityClass, p_helper);

		fields = p_entityClass.getDeclaredFields();
		log.info("Campos Declarados: " + fields);

		mapExtraAttributeFromSlaveField = new HashMap<>(getAllNameExtraAttributesFromFieldToString().size());

	}

	private void createAttribute(final ArrayList<Attribute> l_atts, final Field l_field,
			final ArrayList<Field> p_fieldsForGetExtraAttributes) {
		Attribute l_att = null;
		final Class<?> l_typeField = l_field.getType();
		final String l_nameField = l_field.getName();
		if (helper.ignoreFieldsTypeOf(l_typeField) || helper.ignoreFieldsName(l_nameField)
				|| l_nameField.equals("serialVersionUID")) {
			return;
		}

		if (l_field.getDeclaredAnnotation(Column.class) != null) {
			l_att = createAttributeFromColumn(l_field);
		} else if (l_field.getDeclaredAnnotation(ManyToOne.class) != null) {
			if (!hasExtraAttributeCallBackToField(l_field)) {
				l_att = createAttributeFromManyToOne(l_field);
			} else {
				p_fieldsForGetExtraAttributes.add(l_field);
				return;
			}
		} else if (l_field.getDeclaredAnnotation(OneToMany.class) != null) {
			l_att = createAttributeFromOneToMany(l_field);
			return;
		} else if (l_field.getDeclaredAnnotation(OneToOne.class) != null) {
			l_att = createAttributeFromOneToOne(l_field);
			return;
		} else if (l_field.getDeclaredAnnotation(ManyToMany.class) != null) {
			l_att = createAttributeFromManyToMany(l_field);
			return;
		} else if (l_field.getDeclaredAnnotation(Temporal.class) != null) {
			l_att = createAttributeFromTemporal(l_field);
			return;
		} else {
			return;
		}
		l_atts.add(l_att);
		putAttributeToField(l_att, l_field);
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
		final String l_fieldName = p_field.getName();

		final CallbackField<?> l_callback = getCallbackClassField(p_field);

		Attribute l_att = null;
		if (l_callback instanceof CallbackFieldToNumber) {

			final String l_name = createNameAttribute(p_field);

			l_att = new Attribute(l_name);

		} else {
			final List<String> l_refValues = new ArrayList<>();

			// TODO CRIAR CALLBACK PARA OBTER LISTA, VEJA A LISTA DE TAREFAS.
			final String l_qlString = "SELECT E FROM " + p_field.getType().getSimpleName() + " E ";
			final Query l_query = createQuery(l_qlString);

			@SuppressWarnings("unchecked")
			final List<E> l_list = l_query.getResultList();

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
			l_att = createAttributeFromManyToOne(p_field, l_refValues);
		}

		return l_att;
	}

	/**
	 * Cria um atributo com base em uma relação de muitos para um, esta relação
	 * leva a criar um atributo do tipo Nominal, para isto é preciso de uma
	 * relação de strings na mesma ordem.
	 *
	 * @param p_field
	 * @param p_refListValue
	 * @return
	 */
	private Attribute createAttributeFromManyToOne(Field p_field, List<String> p_refListValue) {
		final String l_name = createNameAttribute(p_field);

		final Attribute l_att = new Attribute(l_name, p_refListValue);
		helper.putAttributeToRefValues(l_att, p_refListValue);
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
		createAttributes(l_atts);

		return l_atts;
	}

	private void createAttributes(final ArrayList<Attribute> l_atts) {
		final ArrayList<Field> l_fieldsForGetExtraAttributes = new ArrayList<Field>(fields.length);

		for (final Field l_field : fields) {
			createAttribute(l_atts, l_field, l_fieldsForGetExtraAttributes);
		}

		log.info("Atributos: " + l_atts);

		createExtraAttributesFromFields(l_fieldsForGetExtraAttributes, l_atts);
		log.info("Atributos: " + l_atts);

		createAttributesExtraFields(l_atts);

		log.info("Lista Final dos Atributos: " + l_atts);
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
			putAttributeToExtraField(l_att, l_extraField);
		}
		log.info("Novos Atributos, Lista geral: " + p_atts);
	}

	@SuppressWarnings("unchecked")
	private void createExtraAttributeFromField(ArrayList<Attribute> p_atts, String p_extraAttribute,
			@SuppressWarnings("rawtypes") Class p_type) {
		// TODO Auto-generated method stub

		Attribute l_att;
		if (p_type.isAssignableFrom(String.class)) {
			l_att = new Attribute(p_extraAttribute, (List<String>) null);
		} else {
			throw new NotImplementedException("O Atributo: " + p_extraAttribute + " usa um tipo (" + p_type
					+ ") no qual não estou preparado para gerar ainda!");
		}
		p_atts.add(l_att);
		mapExtraAttributeFromSlaveField.put(l_att, p_extraAttribute);
	}

	@SuppressWarnings("unchecked")
	private void createExtraAttributesFromFields(ArrayList<Field> p_fieldsForGetExtraAttributes,
			ArrayList<Attribute> p_atts) {

		for (final Field l_field : p_fieldsForGetExtraAttributes) {

			@SuppressWarnings("rawtypes")
			final ExtraAttributesFromFieldToString l_callBacks = getExtraAttributesCallBackToField(l_field);

			l_callBacks.forEachNewAttribute((p_extraAttributeName) -> {

				final String l_extraAttributeName = (String) p_extraAttributeName;
				// TODO identificar uma forma de definir se será String, Numeral
				// ou Nominal
				createExtraAttributeFromField(p_atts, l_extraAttributeName, String.class);

			});
		}
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

	private Set<String> getAllNameExtraAttributesFromFieldToString() {

		// reserva um espaço duas vezese maior que o espaço usado para a chaves
		// no mapa
		final Set<String> l_allExtraAttributes = new HashSet(sizeOfExtraAttributesFromFieldToString() * 2);

		for (final ExtraAttributesFromFieldToString l_extraAttributes : valuesExtraAttributesFromFieldToString()) {
			l_allExtraAttributes.addAll(l_extraAttributes.getExtraAttributes());
		}
		return l_allExtraAttributes;
	}

	/**
	 * Retorna a Classe padrão de um determinado campo extra informado.
	 *
	 * @param p_field
	 * @return
	 */
	private Class<?> getDefaultClassExtraField(String p_field) {
		return helper.getDefaultClassExtraField(p_field);
	}

	private ExtraAttributesFromFieldToString getExtraAttributesCallBackToField(Field p_fieldName) {

		return getExtraAttributesCallBackToField(p_fieldName.getName());
	}

	/**
	 * Retorna os callbacks que serão usados para construir o Attributos Extras,
	 * ele não existindo indica que este campo não tem campo extra, retornando
	 * uma lista vazia.
	 *
	 * @param p_fieldName
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private ExtraAttributesFromFieldToString getExtraAttributesCallBackToField(String p_fieldName) {
		return helper.getExtraAttributesCallBackToField(p_fieldName);
	}

	private Set<String> getExtraFieldsNames() {
		return helper.getExtraFieldsNames();
	}

	/**
	 * Informa se o o campo tem callback cadastrado para obtenção de atributos
	 * extras.
	 *
	 *
	 *
	 * @param p_field
	 * @return
	 */
	private boolean hasExtraAttributeCallBackToField(Field p_field) {

		return hasExtraAttributeCallBackToField(p_field.getName());
	}

	/**
	 * @see #hasExtraAttributeCallBackToField(Field)
	 *
	 * @param p_fieldName
	 * @return
	 */
	private boolean hasExtraAttributeCallBackToField(String p_fieldName) {

		return helper.containFieldNameForExtraAttributesFromFieldToString(p_fieldName);
	}

	private String putAttributeToExtraField(Attribute l_att, final String l_extraField) {
		return helper.putAttributeToExtraField(l_att, l_extraField);
	}

	private Field putAttributeToField(Attribute l_att, final Field l_field) {
		return helper.putAttributeToField(l_att, l_field);
	}

	private int sizeOfExtraAttributesFromFieldToString() {
		return helper.sizeOfExtraAttributesFromFieldToString();
	}

	private Collection<ExtraAttributesFromFieldToString> valuesExtraAttributesFromFieldToString() {
		return helper.valuesFromExtraAttributesFromFieldToString();
	}
}
