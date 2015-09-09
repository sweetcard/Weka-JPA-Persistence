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
 *            Tipo do retorno, obrigatório String
 * @param <V>
 *            Tipo do valor referente ao campo
 */
@FunctionalInterface
public interface CallbackFieldToString  extends CallbackField<String> {
 
}
