package org.weka.jpa.utils;

/**
 * A interface {@link CallbackFieldToString} define o formato do callback
 * (lambda) que será usado para processamento especializado de certos campos que
 * deverão retornar como String.
 * 
 * {@link CallbackFieldToString} é definida com um campos genéricos:
 * 
 * O segundo (V) genérico é o tipo de objeto que o {@link CallbackFieldToString}
 * irá manipular no método {@link CallbackFieldToString#call(String, V)}.
 * 
 * 
 * @author Carlos Delfino {consultoria@carlosdelfino.eti.br, Aminadabe B. Souza
 *         {aminadabebs@gmail.com} e Carlos Barros {carlos.barros22@gmail.com}
 *
 * @param <R>
 *            Tipo do retorno, obrigatório  String
 * @param <V>
 *            Tipo do valor referente ao campo
 */
@FunctionalInterface
public interface CallbackFieldToString<V> extends CallbackField<String, V> {

	/**
	 * O método {@link #call(String, V)} é chamado quando necessário manipular o
	 * campo no qual este {@link CallbackFieldToString} foi registrado este
	 * método recebe o nome do campo que forneceu o valor e o valor que deverá
	 * ser manipullado, que será do tipo V (Generic).
	 * 
	 * @param p_fieldName
	 *            Nome do campo que está fornecendo o valor
	 * @param p_fieldValue
	 *            Valor do campo a ser manipulado
	 * 
	 * @return um valor do tipo R que deve ser {@link String} ou um tipo
	 *         {@link Number}
	 */
	public String call(String p_fieldName, V p_fieldValue);
}
