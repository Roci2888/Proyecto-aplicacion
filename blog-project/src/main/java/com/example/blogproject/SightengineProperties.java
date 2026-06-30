package com.example.blogproject;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sightengine.api")
public class SightengineProperties {
    private String user;
    private String secret;
    private String url;
    private String models;

    // Getters y Setters
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getModels() { return models; }
    public void setModels(String models) { this.models = models; }
}


