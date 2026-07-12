package com.example.blogproject.infrastructure.moderation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SightengineResponse {

    private String status;
    private Nudity nudity;
    private Violence violence; // <-- NUEVO: Atributo para capturar el análisis de violencia

    // Getters y Setters principales
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Nudity getNudity() { return nudity; }
    public void setNudity(Nudity nudity) { this.nudity = nudity; }

    public Violence getViolence() { return violence; } // <-- NUEVO
    public void setViolence(Violence violence) { this.violence = violence; } // <-- NUEVO

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Nudity {
        private Double sexual;
        @JsonProperty("waving_nudity")
        private Double wavingNudity;
        private Double safe;

        public Double getSexual() { return sexual; }
        public void setSexual(Double sexual) { this.sexual = sexual; }
        public Double getWavingNudity() { return wavingNudity; }
        public void setWavingNudity(Double wavingNudity) { this.wavingNudity = wavingNudity; }
        public Double getSafe() { return safe; }
        public void setSafe(Double safe) { this.safe = safe; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Violence {
        private Double weapon;       // Probabilidad de presencia de armas (cuchillos, pistolas)
        private Double injury;       // Probabilidad de heridas físicas visibles o sangre (Gore)
        @JsonProperty("graphic_violence")
        private Double graphicViolence; // Probabilidad de violencia gráfica explícita o peleas

        // Getters y Setters
        public Double getWeapon() { return weapon; }
        public void setWeapon(Double weapon) { this.weapon = weapon; }

        public Double getInjury() { return injury; }
        public void setInjury(Double injury) { this.injury = injury; }

        public Double getGraphicViolence() { return graphicViolence; }
        public void setGraphicViolence(Double graphicViolence) { this.graphicViolence = graphicViolence; }
    }
}