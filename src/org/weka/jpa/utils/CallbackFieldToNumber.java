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
public interface CallbackFieldToNumber<R extends Number>  extends CallbackField<R>  {

	 
}
