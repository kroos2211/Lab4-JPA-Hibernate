package com.example;

import com.example.model.singletable.*;
import com.example.model.joined.*;
import com.example.model.tableperclass.*;
import javax.persistence.*;
import java.util.List;
import java.util.function.Consumer;

public final class App {
    private App() { }

    public static void main(String[] args) {
        EntityManagerFactory factory = Persistence.createEntityManagerFactory("hibernate-inheritance");
        EntityManager em = factory.createEntityManager();
        try {
            singleTable(em);
            joined(em);
            tablePerClass(em);
            compareSql(em);
            System.out.println("Application terminee avec succes.");
        } catch (RuntimeException ex) {
            System.err.println("Erreur pendant la demonstration JPA : " + ex.getMessage());
            throw ex;
        } finally {
            em.close();
            factory.close();
        }
    }

    private static void singleTable(EntityManager em) {
        System.out.println("=== STRATEGIE SINGLE_TABLE ===");
        Voiture voiture = new Voiture("Toyota", 4);
        Moto moto = new Moto("Harley", true);
        inTransaction(em, manager -> {
            manager.persist(voiture);
            manager.persist(moto);
        });
        System.out.println("CREATE : Voiture : " + voiture.getMarque() + " (" + voiture.getNombrePortes() + " portes)");
        System.out.println("CREATE : Moto : " + moto.getMarque() + " (sidecar=" + moto.getSidecar() + ")");
        em.clear();
        System.out.println("READ par id : " + em.find(Voiture.class, voiture.getId()));
        printPolymorphic("Vehicule", em.createQuery("select v from Vehicule v order by v.id", Vehicule.class).getResultList());
        inTransaction(em, manager -> manager.find(Voiture.class, voiture.getId()).setNombrePortes(5));
        em.clear();
        System.out.println("UPDATE : Toyota, nombrePortes=" + em.find(Voiture.class, voiture.getId()).getNombrePortes());
        inTransaction(em, manager -> manager.remove(manager.find(Moto.class, moto.getId())));
        em.clear();
        System.out.println("DELETE : Harley, reste " + count(em, "Vehicule") + " vehicule");
    }

    private static void joined(EntityManager em) {
        System.out.println("=== STRATEGIE JOINED ===");
        Developpeur developpeur = new Developpeur("Alice", "Java");
        Manager manager = new Manager("Bob", 3);
        inTransaction(em, entityManager -> {
            entityManager.persist(developpeur);
            entityManager.persist(manager);
        });
        System.out.println("CREATE : Developpeur : " + developpeur.getNom() + " (" + developpeur.getLangage() + ")");
        System.out.println("CREATE : Manager : " + manager.getNom() + " (" + manager.getNombreEquipes() + " equipes)");
        em.clear();
        System.out.println("READ par id : " + em.find(Developpeur.class, developpeur.getId()));
        printPolymorphic("Employe", em.createQuery("select e from Employe e order by e.id", Employe.class).getResultList());
        inTransaction(em, entityManager -> entityManager.find(Manager.class, manager.getId()).setNombreEquipes(4));
        em.clear();
        System.out.println("UPDATE : Bob, nombreEquipes=" + em.find(Manager.class, manager.getId()).getNombreEquipes());
        inTransaction(em, entityManager -> entityManager.remove(entityManager.find(Developpeur.class, developpeur.getId())));
        em.clear();
        System.out.println("DELETE : Alice, reste " + count(em, "Employe") + " employe");
    }

    private static void tablePerClass(EntityManager em) {
        System.out.println("=== STRATEGIE TABLE_PER_CLASS ===");
        Livre livre = new Livre("Clean Code", "Robert C. Martin");
        Electronique electronique = new Electronique("Smartphone", 24);
        inTransaction(em, manager -> {
            manager.persist(livre);
            manager.persist(electronique);
        });
        System.out.println("CREATE : Livre : " + livre.getNom() + " (" + livre.getAuteur() + ")");
        System.out.println("CREATE : Electronique : " + electronique.getNom() + " (" + electronique.getGarantie() + " mois)");
        em.clear();
        System.out.println("READ par id : " + em.find(Livre.class, livre.getId()));
        printPolymorphic("Produit", em.createQuery("select p from Produit p order by p.id", Produit.class).getResultList());
        inTransaction(em, manager -> manager.find(Electronique.class, electronique.getId()).setGarantie(36));
        em.clear();
        System.out.println("UPDATE : Smartphone, garantie=" + em.find(Electronique.class, electronique.getId()).getGarantie() + " mois");
        inTransaction(em, manager -> manager.remove(manager.find(Livre.class, livre.getId())));
        em.clear();
        System.out.println("DELETE : Clean Code, reste " + count(em, "Produit") + " produit");
    }

    private static void compareSql(EntityManager em) {
        em.clear();
        System.out.println("=== COMPARAISON SQL ===");
        System.out.println("SINGLE_TABLE : requete polymorphique Vehicule");
        em.createQuery("select v from Vehicule v", Vehicule.class).getResultList();
        System.out.println("JOINED : requete polymorphique Employe");
        em.createQuery("select e from Employe e", Employe.class).getResultList();
        System.out.println("TABLE_PER_CLASS : requete polymorphique Produit");
        em.createQuery("select p from Produit p", Produit.class).getResultList();
    }

    private static void printPolymorphic(String hierarchy, List<?> items) {
        System.out.println("READ polymorphe " + hierarchy + " : " + items);
    }

    private static long count(EntityManager em, String name) {
        return em.createQuery("select count(e) from " + name + " e", Long.class).getSingleResult();
    }

    private static void inTransaction(EntityManager em, Consumer<EntityManager> work) {
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            work.accept(em);
            transaction.commit();
        } catch (RuntimeException ex) {
            if (transaction.isActive()) transaction.rollback();
            throw ex;
        }
    }
}
