package com.givinghands.givinghands.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column
    private String password;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "oauth_provider", length = 50)
    private String oauthProvider;

    @Column(nullable = false, length = 20)
    private String role; // "USER", "ORGANIZATION", "ADMIN"

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public String getOauthProvider() { return oauthProvider; }
    public void setOauthProvider(String oauthProvider) { this.oauthProvider = oauthProvider; }
}
