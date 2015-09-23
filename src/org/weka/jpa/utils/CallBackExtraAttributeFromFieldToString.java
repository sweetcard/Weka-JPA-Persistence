/**
 *
 */
package org.weka.jpa.utils;

/**
 * Este CallBack permite manipular um objeto que pertença a entidade principal,
 * gerado novos atributos com base em seus proprios campos
 *
 * T deve ser o tipo do objeto que contem o campo que será criado como atributo
 * do arquivo ARFF
 *
 * @author Carlos Delfino {consultoria@carlosdelfino.eti.br, Aminadabe B. Souza
 *         {aminadabebs@gmail.com} e Carlos Barros {carlos.barros22@gmail.com}
 *
 */
@FunctionalInterface
public interface CallBackExtraAttributeFromFieldToString<T> {

	/**
	 *
	 *
	 *
	 * @param p_entity
	 *            Entidade a qual pertence o campo.
	 *
	 * @param p_newAttribute
	 *            Nome do Attributo que será criado
	 *
	 * @param p_baseFieldName
	 *            Nome do do campo que dá origem a entidade que será manipulada
	 *            para obter o novo
	 *
	 * @param p_value
	 *            Valor do campo a ser manipulado
	 *
	 * @return um valor do tipo R que deve ser {@link String} ou um tipo
	 *         {@link Number}
	 */
	public String call(Object p_entity, String p_newAttribute, String p_baseFieldName, T p_value);

}
