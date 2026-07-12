package com.example.blogproject;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SightengineResponse {

    private String status;

    private Nudity nudity;
    private Violence violence;

    @JsonProperty("recreational_drug")
    private RecreationalDrug recreationalDrug;

    private Medical medical;

    /**
     * Clase base genérica para las categorías que comparten la misma forma:
     * un "prob" general y un objeto "classes" con el detalle por sub-clase.
     * Violence, RecreationalDrug y Medical extienden de aquí.
     */
    @Getter
    @Setter
    public abstract static class ScoredCategory<C> {
        private Double prob;
        private C classes;
    }

    @Getter
    @Setter
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
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Violence extends ScoredCategory<Violence.Classes> {

        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Classes {

            @JsonProperty("physical_violence")
            private Double physicalViolence;

            @JsonProperty("firearm_threat")
            private Double firearmThreat;

            @JsonProperty("combat_sport")
            private Double combatSport;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecreationalDrug extends ScoredCategory<RecreationalDrug.Classes> {

        @Getter
        @Setter
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
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Medical extends ScoredCategory<Medical.Classes> {

        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Classes {
            private Double pills;
            private Double paraphernalia;
        }
    }
}