package com.example.model.singletable;

import javax.persistence.Entity;
import javax.persistence.DiscriminatorValue;

@Entity
@DiscriminatorValue("MOTO")
public class Moto extends Vehicule {
    private boolean sidecar;

    public Moto() { }
    public Moto(String marque, boolean sidecar) {
        super(marque);
        this.sidecar = sidecar;
    }

    public boolean getSidecar() { return sidecar; }
    public void setSidecar(boolean sidecar) { this.sidecar = sidecar; }

    @Override public String toString() {
        return "Moto{id=" + getId() + ", marque='" + getMarque()
                + "', sidecar=" + sidecar + "}";
    }
}
