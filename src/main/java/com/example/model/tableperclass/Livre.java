package com.example.model.tableperclass;

import javax.persistence.Entity;

@Entity
public class Livre extends Produit {
    private String auteur;

    public Livre() { }
    public Livre(String nom, String auteur) {
        super(nom);
        this.auteur = auteur;
    }

    public String getAuteur() { return auteur; }
    public void setAuteur(String auteur) { this.auteur = auteur; }

    @Override public String toString() {
        return "Livre{id=" + getId() + ", nom='" + getNom()
                + "', auteur=" + auteur + "}";
    }
}
