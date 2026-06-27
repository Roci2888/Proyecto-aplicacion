package com.example.blogproject.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "posts")
public class Post {

    @Id
    private String id;

    private String blogId;
    private String title;
    private String content;
    private String imageUrl;
    private String imageCaption;
    private LocalDateTime createdAt;

    public Post() {
    }


    public Post(String blogId, String title, String content,
                String imageUrl, String imageCaption,
                LocalDateTime createdAt) {
        this.blogId = blogId;
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.imageCaption = imageCaption;
        this.createdAt = createdAt;
    }

    // 🔹 GETTERS

    public String getId() {
        return id;
    }

    public String getBlogId() {
        return blogId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getImageCaption() {
        return imageCaption;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // 🔹 SETTERS

    public void setId(String id) {
        this.id = id;
    }

    public void setBlogId(String blogId) {
        this.blogId = blogId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setImageCaption(String imageCaption) {
        this.imageCaption = imageCaption;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    //  toString

    @Override
    public String toString() {
        return "Post{" +
                "id='" + id + '\'' +
                ", blogId='" + blogId + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", imageCaption='" + imageCaption + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}