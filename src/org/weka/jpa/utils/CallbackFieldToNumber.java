package org.weka.jpa.utils;

/**
 * A interface {@link CallbackFieldToNumber} define o formato do callback
 * (lambda) que será usado para processamento especializado de certos campos que
 * deverão retornar como Number, preferencialmente {@link Double}.
 * 
 * {@link CallbackFieldToNumber} é definida com dois campos genéricos:
 * 
 * O primeiro (R) o tipo do valor que será retornado, obrigatóriamente algum
 * tipo de {@link Number},
 * 
 * O segundo (V) genérico é o tipo de objeto que o {@link CallbackFieldToNumber}
 * irá manipular no método {@link CallbackFieldToNumber#call(String, V)}.
 * 
 * 
 * @author Carlos Delfino {consultoria@carlosdelfino.eti.br, Aminadabe B. Souza
 *         {aminadabebs@gmail.com} e Carlos Barros {carlos.barros22@gmail.com}
 *
 * @param <R>
 *            Tipo do retorno, obrigatório Number ou String
 * @param <V>
 *            Tipo do valor referente ao campo
 */
@FunctionalInterface
public interface CallbackFieldToNumber<E, R extends Number, V> extends CallbackField<E, R, V> {

	/**
	 * O método {@link #call(String, V)} é chamado quando necessário manipular o
	 * campo no qual este {@link CallbackFieldToNumber} foi registrado este
	 * método recebe o nome do campo que forneceu o valor e o valor que deverá
	 * ser manipullado, que será do tipo V (Generic).
	 * 
	 * @param p_entity
	 *            Entidade a qual pertence o campo e o valor obtido no campo.
	 * @param p_fieldName
	 *            Nome do campo que está fornecendo o valor
	 * @param p_fieldValue
	 *            Valor do campo a ser manipulado
	 * 
	 * @return um valor do tipo R que deve ser {@link String} ou um tipo
	 *         {@link Number}
	 */
	public R call(E p_entity, String p_fieldName, V p_fieldValue);
}
