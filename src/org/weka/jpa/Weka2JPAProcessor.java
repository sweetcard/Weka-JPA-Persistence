package org.weka.jpa;

import java.lang.reflect.Field;

import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weka.jpa.utils.CallbackField;

public abstract class Weka2JPAProcessor<E> {
	protected final String relationBaseName;
	protected final Class<E> entityClass;
	protected final Weka2JPAHelper<E> helper;

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	public Weka2JPAProcessor(Class<E> p_entityClass, Weka2JPAHelper<E> p_helper) {
		entityClass = p_entityClass;
		helper = p_helper;
		relationBaseName = p_entityClass.getSimpleName();
		log.info("Instancias Relacionadas a " + relationBaseName);
	}

	protected Query createQuery(String p_qlString) {
		return helper.createQuery(p_qlString);
	}

	/**
	 * Obtem o CallBack para um determinado campo de uma entidade com base no
	 * nome do campo
	 *
	 * @param l_fieldName
	 * @return
	 */
	private CallbackField<?> getCallBackClass(String l_fieldName) {

		final CallbackField<?> l_callback = helper.getBaseClassFieldClassCallBack(l_fieldName);
		return l_callback;
	}

	private CallbackField<?> getCallbackClassField(Class<?> p_class) {
		return helper.getBaseClassFieldClassCallBack(p_class);
	}

	/**
	 * Obtem o CallBack para um determinado campo da entidade.
	 *
	 * @param p_field
	 * @return
	 */
	protected CallbackField<?> getCallbackClassField(Field p_field) {
		final String l_fieldName = p_field.getName();
		CallbackField<?> l_callback = getCallBackClass(l_fieldName);
		if (l_callback == null) {
			l_callback = getCallbackClassField(p_field.getType());
		}
		return l_callback;
	}

	protected CallbackField<?> getCallBackExtraField(String p_fieldName) {

		return helper.getCallBackExtraField(p_fieldName);
	}

	/**
	 * Retorna o valor padrão para um determinado campo.
	 *
	 * @param l_fieldName
	 * @return
	 */
	protected Object getDefaultValue(String p_fieldName) {
		return helper.getDefaultValue(p_fieldName);
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

}
