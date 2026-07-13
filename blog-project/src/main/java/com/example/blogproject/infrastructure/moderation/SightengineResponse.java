package com.example.blogproject.infrastructure.moderation;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SightengineResponse {

    private String status;

    private Nudity nudity;
    private Violence violence;

    @JsonProperty("recreational_drug")
    private RecreationalDrug recreationalDrug;

    private Medical medical;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Nudity getNudity() {
        return nudity;
    }

    public void setNudity(Nudity nudity) {
        this.nudity = nudity;
    }

    public Violence getViolence() {
        return violence;
    }

    public void setViolence(Violence violence) {
        this.violence = violence;
    }

    public RecreationalDrug getRecreationalDrug() {
        return recreationalDrug;
    }

    public void setRecreationalDrug(RecreationalDrug recreationalDrug) {
        this.recreationalDrug = recreationalDrug;
    }

    public Medical getMedical() {
        return medical;
    }

    public void setMedical(Medical medical) {
        this.medical = medical;
    }

    /**
     * Clase base genérica para las categorías que comparten la misma forma:
     * un "prob" general y un objeto "classes" con el detalle por sub-clase.
     * Violence, RecreationalDrug y Medical extienden de aquí.
     */
    public abstract static class ScoredCategory<C> {
        private Double prob;
        private C classes;

        public Double getProb() {
            return prob;
        }

        public void setProb(Double prob) {
            this.prob = prob;
        }

        public C getClasses() {
            return classes;
        }

        public void setClasses(C classes) {
            this.classes = classes;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Nudity {

        @JsonProperty("sexual_activity")
        private Double sexualActivity;

        @JsonProperty("sexual_display")
        private Double sexualDisplay;

        private Double erotica;

        @JsonProperty("very_suggestive")
        private Double verySuggestive;

        private Double suggestive;

        @JsonProperty("mildly_suggestive")
        private Double mildlySuggestive;

        private Double none;

        @JsonIgnore
        private Object suggestiveClasses;

        private Map<String, Double> context;

        public Double getSexualActivity() {
            return sexualActivity;
        }

        public void setSexualActivity(Double sexualActivity) {
            this.sexualActivity = sexualActivity;
        }

        public Double getSexualDisplay() {
            return sexualDisplay;
        }

        public void setSexualDisplay(Double sexualDisplay) {
            this.sexualDisplay = sexualDisplay;
        }

        public Double getErotica() {
            return erotica;
        }

        public void setErotica(Double erotica) {
            this.erotica = erotica;
        }

        public Double getVerySuggestive() {
            return verySuggestive;
        }

        public void setVerySuggestive(Double verySuggestive) {
            this.verySuggestive = verySuggestive;
        }

        public Double getSuggestive() {
            return suggestive;
        }

        public void setSuggestive(Double suggestive) {
            this.suggestive = suggestive;
        }

        public Double getMildlySuggestive() {
            return mildlySuggestive;
        }

        public void setMildlySuggestive(Double mildlySuggestive) {
            this.mildlySuggestive = mildlySuggestive;
        }

        public Double getNone() {
            return none;
        }

        public void setNone(Double none) {
            this.none = none;
        }

        public Object getSuggestiveClasses() {
            return suggestiveClasses;
        }

        public void setSuggestiveClasses(Object suggestiveClasses) {
            this.suggestiveClasses = suggestiveClasses;
        }

        public Map<String, Double> getContext() {
            return context;
        }

        public void setContext(Map<String, Double> context) {
            this.context = context;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Violence extends ScoredCategory<Violence.Classes> {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Classes {

            @JsonProperty("physical_violence")
            private Double physicalViolence;

            @JsonProperty("firearm_threat")
            private Double firearmThreat;

            @JsonProperty("combat_sport")
            private Double combatSport;

            public Double getPhysicalViolence() {
                return physicalViolence;
            }

            public void setPhysicalViolence(Double physicalViolence) {
                this.physicalViolence = physicalViolence;
            }

            public Double getFirearmThreat() {
                return firearmThreat;
            }

            public void setFirearmThreat(Double firearmThreat) {
                this.firearmThreat = firearmThreat;
            }

            public Double getCombatSport() {
                return combatSport;
            }

            public void setCombatSport(Double combatSport) {
                this.combatSport = combatSport;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecreationalDrug extends ScoredCategory<RecreationalDrug.Classes> {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Classes {

            private Double cannabis;

            @JsonProperty("cannabis_logo_only")
            private Double cannabisLogoOnly;

            @JsonProperty("cannabis_plant")
            private Double cannabisPlant;

            @JsonProperty("cannabis_drug")
            private Double cannabisDrug;

            @JsonAlias({"recreational_drugs_not_cannabis", "recreational_drug_not_cannabis"})
            private Double recreationalDrugsNotCannabis;

            public Double getCannabis() {
                return cannabis;
            }

            public void setCannabis(Double cannabis) {
                this.cannabis = cannabis;
            }

            public Double getCannabisLogoOnly() {
                return cannabisLogoOnly;
            }

            public void setCannabisLogoOnly(Double cannabisLogoOnly) {
                this.cannabisLogoOnly = cannabisLogoOnly;
            }

            public Double getCannabisPlant() {
                return cannabisPlant;
            }

            public void setCannabisPlant(Double cannabisPlant) {
                this.cannabisPlant = cannabisPlant;
            }

            public Double getCannabisDrug() {
                return cannabisDrug;
            }

            public void setCannabisDrug(Double cannabisDrug) {
                this.cannabisDrug = cannabisDrug;
            }

            public Double getRecreationalDrugsNotCannabis() {
                return recreationalDrugsNotCannabis;
            }

            public void setRecreationalDrugsNotCannabis(Double recreationalDrugsNotCannabis) {
                this.recreationalDrugsNotCannabis = recreationalDrugsNotCannabis;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Medical extends ScoredCategory<Medical.Classes> {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Classes {
            private Double pills;
            private Double paraphernalia;

            public Double getPills() {
                return pills;
            }

            public void setPills(Double pills) {
                this.pills = pills;
            }

            public Double getParaphernalia() {
                return paraphernalia;
            }

            public void setParaphernalia(Double paraphernalia) {
                this.paraphernalia = paraphernalia;
            }
        }
    }
}
