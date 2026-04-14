package com.example.demo.usuario;

import java.time.LocalDateTime;

public class Direccion {

    private String calle;
    private String ciudad;
    private String region;
    private String telefono;
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    public Direccion() {}

    public Direccion(String calle, String ciudad, String region, String telefono) {
        this.calle = calle;
        this.ciudad = ciudad;
        this.region = region;
        this.telefono = telefono;
        this.fechaRegistro = LocalDateTime.now();
    }

    public String getCalle() { return calle; }
    public void setCalle(String calle) { this.calle = calle; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
