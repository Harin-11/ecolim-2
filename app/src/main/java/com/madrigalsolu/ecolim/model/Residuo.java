package com.madrigalsolu.ecolim.model;

import java.io.Serializable;

/**
 * Entidad que representa un registro de recolección de residuos.
 * Implementa Serializable para comunicación entre componentes y Firebase.
 */
public class Residuo implements Serializable {

    private long id;
    private String tipoResiduo;      // Ej: "Orgánico", "Plástico", "Papel/Cartón", "Vidrio", "Peligroso", "Metal"
    private double cantidadKg;       // Cantidad en kilogramos
    private String ubicacion;        // Ambiente / punto de recolección
    private String fecha;            // formato yyyy-MM-dd
    private String hora;             // formato HH:mm
    private String observaciones;    // notas adicionales
    private int sincronizado;        // 0 = pendiente, 1 = sincronizado en la nube
    private String firebaseKey;      // clave única en Firebase Realtime Database

    public Residuo() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTipoResiduo() {
        return tipoResiduo;
    }

    public void setTipoResiduo(String tipoResiduo) {
        this.tipoResiduo = tipoResiduo;
    }

    public double getCantidadKg() {
        return cantidadKg;
    }

    public void setCantidadKg(double cantidadKg) {
        this.cantidadKg = cantidadKg;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public int getSincronizado() {
        return sincronizado;
    }

    public void setSincronizado(int sincronizado) {
        this.sincronizado = sincronizado;
    }

    public String getFirebaseKey() {
        return firebaseKey;
    }

    public void setFirebaseKey(String firebaseKey) {
        this.firebaseKey = firebaseKey;
    }
}
