package org.weka.jpa;

/**
 * Quando informando um campo que não existe, é lançada a exception.
 *
 * @author Carlos Delfino {consultoria@carlosdelfino.eti.br, Aminadabe B. Souza
 *         {aminadabebs@gmail.com} e Carlos Barros {carlos.barros22@gmail.com}
 *
 */
public class NotHaveFieldWekaJPARuntimeException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 7552314575120665966L;

	public NotHaveFieldWekaJPARuntimeException(Exception p_e) {
		super(p_e);
	}

}
