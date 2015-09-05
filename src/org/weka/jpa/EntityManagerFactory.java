package org.weka.jpa;
 
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.persistence.EntityManager; 
import javax.persistence.Persistence;

public class EntityManagerFactory {

	@Produces
	@Named("WekaPersistence")
	public EntityManager createEntityManager() {
		String l_unit = System.getProperty("WEKA.PERSISTENCE.UNIT","WekaPersistenceUnit");
		// TODO Get PersistenceUnit
		return Persistence.createEntityManagerFactory(l_unit).createEntityManager();
	}

	public void closeEM(@Disposes EntityManager manager) {
		manager.close();
	}
}
