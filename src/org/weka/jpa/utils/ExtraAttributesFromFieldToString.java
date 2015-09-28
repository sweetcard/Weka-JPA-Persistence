package org.weka.jpa.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weka.jpa.NotHaveFieldWekaJPARuntimeException;

import weka.core.Attribute;
import weka.core.Instances;

/**
 * Classe responsável por armazenar os atributos extras para um determinado
 * Campo.
 *
 * @author Carlos Delfino {consultoria@carlosdelfino.eti.br, Aminadabe B. Souza
 *         {aminadabebs@gmail.com} e Carlos Barros {carlos.barros22@gmail.com}
 *
 */
public class ExtraAttributesFromFieldToString<T> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final String fieldName;
	private final Class<T> baseClass;

	@SuppressWarnings("rawtypes")
	private final Map<String, CallBackExtraAttributeFromFieldToString> map = new HashMap<String, CallBackExtraAttributeFromFieldToString>();

	/**
	 *
	 * @param p_fieldName
	 *            Campo que fornecerá o objeto
	 * @param p_class
	 *            Classe (tipo) do campo
	 *
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	public ExtraAttributesFromFieldToString(String p_fieldName, Class<T> p_class) {
		fieldName = p_fieldName;
		baseClass = p_class;
	}

	public void forEachNewAttribute(Consumer<? super String> p_action) {
		map.keySet().forEach(p_action);
	}

	public void forObjectCreateNewValueForEachAttribute(Object p_object, Instances p_instances, double[] p_vals,
			ArrayList<Attribute> p_atts, ArrayList<Attribute> p_incoginitoAttributes) {
		map.forEach((p_attributeName, p_callBack) -> {
			log.info("Atributo: " + p_attributeName);
			try {
				final String l_fieldName = getFieldName();
				final Class<? extends Object> l_class = p_object.getClass();
				final Field l_declaredField = l_class.getDeclaredField(l_fieldName);
				// caso o campo esteja privado
				l_declaredField.setAccessible(true);
				final Object l_entity = l_declaredField.get(p_object);
				log.info("Conteúdo da Entidade Filha: " + l_entity);
				final String l_attributeValue = p_callBack.call(p_object, p_attributeName, l_fieldName, l_entity);
				log.info("Valor para o novo Atributo:" + l_attributeValue);

				final Attribute l_attribute = p_instances.attribute(p_attributeName);
				final int l_attributeValueIndex = l_attribute.addStringValue(l_attributeValue);
				int l_index = l_attribute.index();
				p_vals[l_index] = l_attributeValueIndex;
			} catch (final Exception e) {
				e.printStackTrace();
				throw new NotHaveFieldWekaJPARuntimeException(e);
			}
		});
	}

	/**
	 * @return the baseClass
	 */
	public Class<T> getBaseClass() {
		return baseClass;
	}

	public Set<String> getExtraAttributes() {
		return map.keySet();
	}

	public Field getField(Object p_object) throws NoSuchFieldException, SecurityException {
		return p_object.getClass().getField(getFieldName());
	}

	/**
	 * @return the fieldName
	 */
	public String getFieldName() {
		return fieldName;
	}

	/**
	 * Obtem o valor do tipo <T> do campo para o objeto informado
	 *
	 * O valor retornado deve ser obrigatóriamente
	 *
	 * @param p_object
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	public T getValueFromFieldAt(Object p_object)
			throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		// TODO: Analisar se é interessante usar também o método get especifico
		// do campo, por exemplo "postData" usar "getPostData" se existir.
		return (T) getField(p_object).get(p_object);
	}

	public CallBackExtraAttributeFromFieldToString put(String p_attributeName,
			CallBackExtraAttributeFromFieldToString<T> p_callback) {
		return map.put(p_attributeName, p_callback);
	}

	@Override
	public String toString() {
		return "No Campo de nome " + fieldName + " do tipo " + getBaseClass().getSimpleName() + " para os atributos: "
				+ map.keySet();
	}

}
