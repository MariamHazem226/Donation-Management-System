package com.givinghands.givinghands.dto;

import com.givinghands.givinghands.entity.User;

/**
 * Safe user payload for API responses (never includes password).
 */
public class UserResponseDTO {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String avatarUrl;
    private String coverUrl;

    public static UserResponseDTO from(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole() != null ? user.getRole().trim().toUpperCase() : "USER");
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setCoverUrl(user.getCoverUrl());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
}
