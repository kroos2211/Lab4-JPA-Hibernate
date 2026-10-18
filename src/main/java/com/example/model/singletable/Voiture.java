package com.example.model.singletable;

import javax.persistence.Entity;
import javax.persistence.DiscriminatorValue;

@Entity
@DiscriminatorValue("VOITURE")
public class Voiture extends Vehicule {
    private int nombrePortes;

    public Voiture() { }
    public Voiture(String marque, int nombrePortes) {
        super(marque);
        this.nombrePortes = nombrePortes;
    }

    public int getNombrePortes() { return nombrePortes; }
    public void setNombrePortes(int nombrePortes) { this.nombrePortes = nombrePortes; }

    @Override public String toString() {
        return "Voiture{id=" + getId() + ", marque='" + getMarque()
                + "', nombrePortes=" + nombrePortes + "}";
    }
}
