package com.example.blogproject.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "blogs")
public class Blog {

    @Id
    private String id;

    private String userId;
    private String name;
    private String description;

    private String status = "DRAFT";

    private String coverImageUrl;

    public Blog() {}

    public Blog(String userId, String name, String description) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.status = "DRAFT";
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getCoverImageUrl() { return coverImageUrl; }

    public boolean isDraft()  { return "DRAFT".equals(status); }
    public boolean isPublic() { return "PUBLIC".equals(status); }

    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(String status) { this.status = status; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    @Override
    public String toString() {
        return "Blog{id='" + id + "', userId='" + userId +
                "', name='" + name + "', status='" + status + "'}";
    }
}