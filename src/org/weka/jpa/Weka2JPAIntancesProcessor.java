package org.weka.jpa;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import javax.persistence.Query;

import org.weka.jpa.utils.CallbackField;
import org.weka.jpa.utils.CallbackFieldToNumber;
import org.weka.jpa.utils.CallbackFieldToString;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class Weka2JPAIntancesProcessor<E> extends Weka2JPAProcessor<E> {

	public Weka2JPAIntancesProcessor(Class<E> p_entityClass, Weka2JPAHelper<E> p_helper) {
		super(p_entityClass, p_helper);
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
		final Object l_value = helper.getMissingValueToFields(p_fieldName);
		if (l_value == null) {
			return false;
		}
		return l_value.equals(l_string);
	}

	public Instances createInstances(ArrayList<Attribute> p_atts, Collection<E> p_list) {
		Collection<E> l_list;
		if (p_list == null) {
			log.info("Instancias obtidos diretamente pelo JPA");
			final String l_qlString = "SELECT E FROM " + getRelationBaseName() + " E ";

			final Query l_query = createQuery(l_qlString);

			l_list = l_query.getResultList();
		} else {
			log.info("Instancias usando lista de entidades fornecida");
			l_list = p_list;
		}

		final Instances l_instances = createInstances(p_atts, l_list);

		return l_instances;
	}

	private void forEachAttributeToExtraField(BiConsumer<? super Attribute, ? super String> p_action) {
		helper.forEachAttributeToExtraField(p_action);
	}

	private void forEachAttributeToField(BiConsumer<? super Attribute, ? super Field> p_action) {
		helper.forEachAttributeToField(p_action);
	}

	private List<String> getReferenceValues(Attribute l_att) {
		return helper.getReferenceValues(l_att);
	}

	private Instances populateInstanceWithData(ArrayList<Attribute> p_atts, Collection<E> p_list) {
		final Instances l_instances = new Instances(getRelationBaseName(), p_atts, 0);
		for (final E l_object : p_list) {
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

		forEachAttributeToExtraField((p_att, p_fieldName) -> {

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

		forEachAttributeToField((p_att, p_field) -> {

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

					Object l_strValue;
					if (l_value instanceof String) {
						l_strValue = l_value;
					} else {
						final CallbackField<?> l_callback = getCallbackClassField(p_field);

						if (l_callback != null) {
							final String l_fieldName = p_field.getName();
							l_strValue = l_callback.call(p_entityObj, l_fieldName, l_value);
						} else {
							l_strValue = l_value.toString();
						}
					}

					if (useNullLikeIncognito() && l_strValue == null
							|| checkMissingFieldValue(p_field.getName(), l_value)) {
						l_incognito = true;
					} else if (l_strValue instanceof String) {
						l_val = l_refValues.indexOf(l_strValue);
					} else if (l_strValue instanceof Number) {
						l_val = ((Number) l_strValue).doubleValue();
					}

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
		return helper.useNullLikeIncognito();
	}

}
