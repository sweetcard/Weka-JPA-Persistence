package org.weka.jpa;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.cdi.utils.ConfigurationInjectionManager;
import org.cdi.utils.InjectedConfiguration;
import org.jboss.weld.log.LoggerProducer;
import org.slf4j.Logger;

public class Weka2JPAHelperFactory {

	@Inject
	ConfigurationInjectionManager cim;

	@Inject
	LoggerProducer lp;

	@Inject
	EntityManagerFactory emf;

	@Produces
	@InjectedConfiguration
	public <E> Weka2JPAHelper<E> createHelper(InjectionPoint p_ip) {

		Logger l_logger = lp.produceLog(p_ip);
		EntityManager l_em = emf.createEntityManager(p_ip);
		return new Weka2JPAHelper<>(l_logger, l_em);
	}
}
