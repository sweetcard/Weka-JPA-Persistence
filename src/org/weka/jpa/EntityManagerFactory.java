package org.weka.jpa;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.cdi.utils.CDIUtilsInjectionManager;
import org.cdi.utils.annotations.InjectedString;

/**
 * 
 * @author Carlos Delfino {consultoria@carlosdelfino.eti.br, Aminadabe B. Souza
 *         {aminadabebs@gmail.com} e Carlos Barros {carlos.barros22@gmail.com}
 *
 */
public class EntityManagerFactory {

	public static final String WEKA_PERSISTENCE = "WekaPersistence";
	public static final String WEKA_PERSISTENCE_CONFIGURED = "WekaPersistenceConfigured";

	/**
	 * 
	 */
	@Inject
	@InjectedString(key = "WEKA.PERSISTENCE.UNIT", defaultValue = WEKA_PERSISTENCE)
	private String unitName;

	@Inject
	private CDIUtilsInjectionManager cim;

	/**
	 * Cria uma {@link EntityManager} com base na configuração injetada, esta
	 * configuração deve ter a chave WEKA.PERSISTENCE.UNIT configurada para a
	 * UNIT que será usada ou deverá estar no arquivo configuration.properties
	 * ou conforme valor padrão fornecido.
	 */	@Produces
	@Any
	@Named(WEKA_PERSISTENCE_CONFIGURED)
	@InjectedString
	public EntityManager createEntityManager(InjectionPoint p_ip) {
		String l_value = cim.injectString(p_ip);
		return Persistence.createEntityManagerFactory(l_value).createEntityManager();
	}

	/**
	 * Cria um EntityManager com base na chave de configuração
	 * WEKA.PERSISTENCE.UNIT que será encontrada no arquivo configure.properties
	 * que deve estar no classpath.
	 * 
	 * @param p_ip
	 * @return
	 */
	@Produces
	@Default
	@Named(WEKA_PERSISTENCE)
	public EntityManager createEntityManager() {
		// String l_value = cim.injectConfiguration(p_ip);
		return Persistence.createEntityManagerFactory(unitName).createEntityManager();
	}

	/**
	 * 
	 * @param manager
	 */
	public void closeEM(@Disposes EntityManager manager) {
		manager.close();
	}
}
