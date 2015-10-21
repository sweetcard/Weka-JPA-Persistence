package org.weka.jpa;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.cdi.utils.CDIUtilsInjectionManager;
import org.cdi.utils.annotations.InjectedString;
import org.jboss.weld.log.LoggerProducer;
import org.slf4j.Logger;

public class Weka2JPAHelperFactory {

	@Inject
	CDIUtilsInjectionManager cim;

	@Inject
	LoggerProducer lp;

	@Inject
	EntityManagerFactory emf;

	@Produces
	@InjectedString
	public <E> Weka2JPAHelper<E> createHelper(InjectionPoint p_ip) {

		Logger l_logger = lp.produceLog(p_ip);
		EntityManager l_em = emf.createEntityManager(p_ip);
		return new Weka2JPAHelper<>(l_logger, l_em);
	}
}
