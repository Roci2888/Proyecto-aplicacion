package com.example.blogproject.infrastructure.moderation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SightengineResponse {

    private String status;
    private Nudity nudity;

    // Campos de nivel superior del modelo "wad" (weapons, alcohol, drugs)
    private Double weapon;
    private Double alcohol;
    private Double drugs;

    // Modelo "gore"
    private Gore gore;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Nudity getNudity() { return nudity; }
    public void setNudity(Nudity nudity) { this.nudity = nudity; }

    public Double getWeapon() { return weapon; }
    public void setWeapon(Double weapon) { this.weapon = weapon; }

    public Double getAlcohol() { return alcohol; }
    public void setAlcohol(Double alcohol) { this.alcohol = alcohol; }

    public Double getDrugs() { return drugs; }
    public void setDrugs(Double drugs) { this.drugs = drugs; }

    public Gore getGore() { return gore; }
    public void setGore(Gore gore) { this.gore = gore; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Nudity {
        private Double raw;
        private Double partial;
        private Double safe;

        public Double getRaw() { return raw; }
        public void setRaw(Double raw) { this.raw = raw; }
        public Double getPartial() { return partial; }
        public void setPartial(Double partial) { this.partial = partial; }
        public Double getSafe() { return safe; }
        public void setSafe(Double safe) { this.safe = safe; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Gore {
        private Double prob;

        public Double getProb() { return prob; }
        public void setProb(Double prob) { this.prob = prob; }
    }
}