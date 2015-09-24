package org.weka.jpa.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Classe responsável por armazenar os atributos extras para um determinado
 * Campo.
 *
 * @author Carlos Delfino {consultoria@carlosdelfino.eti.br, Aminadabe B. Souza
 *         {aminadabebs@gmail.com} e Carlos Barros {carlos.barros22@gmail.com}
 *
 */
public class ExtraAttributesFromFieldToString<T> {

	private final String fieldName;
	private final Class<T> clazz;

	@SuppressWarnings("rawtypes")
	private final Map<String, CallBackExtraAttributeFromFieldToString> map = new HashMap<String, CallBackExtraAttributeFromFieldToString>();

	public ExtraAttributesFromFieldToString(String p_fieldName, Class<T> p_class) {
		fieldName = p_fieldName;
		clazz = p_class;
	}

	/**
	 * Para cada Atributo extra cadastrado excuta uma ação sobre ele e seu
	 * callback.
	 *
	 * @param p_action
	 */
	public void forEach(BiConsumer<? super String, ? super CallBackExtraAttributeFromFieldToString> p_action) {
		map.forEach(p_action);
	}

	/**
	 * @return the clazz
	 */
	public Class<T> getClazz() {
		return clazz;
	}

	public Set<String> getExtraAttributes() {
		return map.keySet();
	}

	/**
	 * @return the fieldName
	 */
	public String getFieldName() {
		return fieldName;
	}

	public CallBackExtraAttributeFromFieldToString put(String p_attributeName,
			CallBackExtraAttributeFromFieldToString<T> p_callback) {
		return map.put(p_attributeName, p_callback);
	}

}
