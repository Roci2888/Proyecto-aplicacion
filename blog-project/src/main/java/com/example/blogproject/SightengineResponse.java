package com.example.blogproject;

import lombok.Data;
import java.util.Map;

@Data
public class SightengineResponse {
    private Map<String, Double> nudity;
    private Map<String, Double> wad;
    private Map<String, Double> offensive;
    private Map<String, Double> gore;
    private String status;
    private String requestId;
    private Double score;
}
