package com.example;

import com.example.model.singletable.*;
import com.example.model.joined.*;
import com.example.model.tableperclass.*;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import javax.persistence.*;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import static org.junit.Assert.*;

public class InheritanceTest {
    private EntityManagerFactory factory;
    private EntityManager em;

    @Before public void setUp() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("javax.persistence.jdbc.url", "jdbc:h2:mem:lab4_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1");
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        properties.put("hibernate.show_sql", "false");
        factory = Persistence.createEntityManagerFactory("hibernate-inheritance", properties);
        em = factory.createEntityManager();
    }

    @After public void tearDown() {
        if (em != null && em.getTransaction().isActive()) em.getTransaction().rollback();
        if (em != null) em.close();
        if (factory != null) factory.close();
    }

    private <T> T save(T entity) {
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        em.clear();
        return entity;
    }

    private boolean tableExists(String table) {
        return em.unwrap(Session.class).doReturningWork(connection -> {
            try (ResultSet tables = connection.getMetaData().getTables(null, "PUBLIC", table.toUpperCase(Locale.ROOT), new String[]{"TABLE"})) {
                return tables.next();
            }
        });
    }

    private boolean columnExists(String table, String column) {
        return em.unwrap(Session.class).doReturningWork(connection -> {
            try (ResultSet columns = connection.getMetaData().getColumns(null, "PUBLIC", table.toUpperCase(Locale.ROOT), column.toUpperCase(Locale.ROOT))) {
                return columns.next();
            }
        });
    }

    private boolean hasForeignKey(String table, String parent) {
        return em.unwrap(Session.class).doReturningWork(connection -> {
            try (ResultSet keys = connection.getMetaData().getImportedKeys(null, "PUBLIC", table.toUpperCase(Locale.ROOT))) {
                while (keys.next()) {
                    if (parent.equalsIgnoreCase(keys.getString("PKTABLE_NAME"))) return true;
                }
                return false;
            }
        });
    }

    @Test public void voitureIsCreated() {
        Voiture voiture = save(new Voiture("Toyota", 4));
        Voiture found = em.find(Voiture.class, voiture.getId());
        assertEquals("Toyota", found.getMarque());
        assertEquals(4, found.getNombrePortes());
    }

    @Test public void motoIsCreated() {
        Moto moto = save(new Moto("Harley", true));
        assertTrue(em.find(Moto.class, moto.getId()).getSidecar());
    }

    @Test public void singleTableDiscriminatorAndStructure() {
        Voiture voiture = save(new Voiture("Toyota", 4));
        Moto moto = save(new Moto("Harley", true));
        assertTrue(tableExists("Vehicule"));
        assertFalse(tableExists("Voiture"));
        assertFalse(tableExists("Moto"));
        assertTrue(columnExists("Vehicule", "vehicule_type"));
        assertEquals("VOITURE", em.createNativeQuery("select vehicule_type from Vehicule where id = :id").setParameter("id", voiture.getId()).getSingleResult());
        assertEquals("MOTO", em.createNativeQuery("select vehicule_type from Vehicule where id = :id").setParameter("id", moto.getId()).getSingleResult());
    }

    @Test public void vehiclePolymorphism() {
        save(new Voiture("Toyota", 4));
        save(new Moto("Harley", true));
        List<Vehicule> found = em.createQuery("select v from Vehicule v", Vehicule.class).getResultList();
        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(Voiture.class::isInstance));
        assertTrue(found.stream().anyMatch(Moto.class::isInstance));
    }

    @Test public void developerIsCreated() {
        Developpeur developer = save(new Developpeur("Alice", "Java"));
        assertEquals("Java", em.find(Developpeur.class, developer.getId()).getLangage());
    }

    @Test public void managerIsCreated() {
        Manager manager = save(new Manager("Bob", 3));
        assertEquals(3, em.find(Manager.class, manager.getId()).getNombreEquipes());
    }

    @Test public void employeePolymorphism() {
        save(new Developpeur("Alice", "Java"));
        save(new Manager("Bob", 3));
        List<Employe> found = em.createQuery("select e from Employe e", Employe.class).getResultList();
        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(Developpeur.class::isInstance));
        assertTrue(found.stream().anyMatch(Manager.class::isInstance));
    }

    @Test public void joinedTablesHaveForeignKeys() {
        assertTrue(tableExists("Employe"));
        assertTrue(tableExists("Developpeur"));
        assertTrue(tableExists("Manager"));
        assertTrue(hasForeignKey("Developpeur", "Employe"));
        assertTrue(hasForeignKey("Manager", "Employe"));
        assertTrue(columnExists("Employe", "nom"));
        assertTrue(columnExists("Developpeur", "langage"));
        assertTrue(columnExists("Manager", "nombreEquipes"));
    }

    @Test public void bookIsCreated() {
        Livre book = save(new Livre("Clean Code", "Robert C. Martin"));
        assertEquals("Robert C. Martin", em.find(Livre.class, book.getId()).getAuteur());
    }

    @Test public void electronicIsCreated() {
        Electronique item = save(new Electronique("Smartphone", 24));
        assertEquals(24, em.find(Electronique.class, item.getId()).getGarantie());
    }

    @Test public void productPolymorphism() {
        save(new Livre("Clean Code", "Robert C. Martin"));
        save(new Electronique("Smartphone", 24));
        List<Produit> found = em.createQuery("select p from Produit p", Produit.class).getResultList();
        assertEquals(2, found.size());
        assertTrue(found.stream().anyMatch(Livre.class::isInstance));
        assertTrue(found.stream().anyMatch(Electronique.class::isInstance));
    }

    @Test public void tablePerClassStructure() {
        assertTrue(tableExists("Livre"));
        assertTrue(tableExists("Electronique"));
        assertFalse(tableExists("Produit"));
        assertTrue(columnExists("Livre", "id"));
        assertTrue(columnExists("Livre", "nom"));
        assertTrue(columnExists("Livre", "auteur"));
        assertTrue(columnExists("Electronique", "id"));
        assertTrue(columnExists("Electronique", "nom"));
        assertTrue(columnExists("Electronique", "garantie"));
    }

    @Test public void updateIsCommitted() {
        Voiture voiture = save(new Voiture("Toyota", 4));
        em.getTransaction().begin();
        em.find(Voiture.class, voiture.getId()).setNombrePortes(5);
        em.getTransaction().commit();
        em.clear();
        assertEquals(5, em.find(Voiture.class, voiture.getId()).getNombrePortes());
    }

    @Test public void deleteIsCommitted() {
        Moto moto = save(new Moto("Harley", true));
        em.getTransaction().begin();
        em.remove(em.find(Moto.class, moto.getId()));
        em.getTransaction().commit();
        em.clear();
        assertNull(em.find(Moto.class, moto.getId()));
    }

    @Test public void rollbackCancelsInsert() {
        em.getTransaction().begin();
        em.persist(new Voiture("Toyota", 4));
        em.getTransaction().rollback();
        em.clear();
        assertEquals(Long.valueOf(0), em.createQuery("select count(v) from Vehicule v", Long.class).getSingleResult());
    }
}
