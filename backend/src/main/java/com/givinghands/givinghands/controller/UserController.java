package com.givinghands.givinghands.controller;

import com.givinghands.givinghands.dto.UserResponseDTO;
import com.givinghands.givinghands.entity.User;
import com.givinghands.givinghands.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    private final Path uploadsDir = Paths.get("uploads", "avatars");

    // GET /api/users/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(UserResponseDTO.from(user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/users/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @RequestBody Map<String, String> request) {
        try {
            User updated = userService.updateUser(id, request.get("name"), request.get("email"));
            updated.setPassword(null);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/users
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(users);
    }

    // DELETE /api/users/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/users/{id}/avatar — upload avatar image
    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatar(@PathVariable Long id,
                                          @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "file is required"));
        try {
            Files.createDirectories(uploadsDir);
            String ext = getExtension(file.getOriginalFilename());
            String filename = "avatar_" + id + "_" + UUID.randomUUID() + ext;
            Path dest = uploadsDir.resolve(filename);
            file.transferTo(dest);
            String url = "/api/users/" + id + "/images/" + filename;
            userService.updateAvatar(id, url);
            return ResponseEntity.ok(Map.of("avatarUrl", url));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Upload failed"));
        }
    }

    // POST /api/users/{id}/cover — upload cover image
    @PostMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCover(@PathVariable Long id,
                                         @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "file is required"));
        try {
            Files.createDirectories(uploadsDir);
            String ext = getExtension(file.getOriginalFilename());
            String filename = "cover_" + id + "_" + UUID.randomUUID() + ext;
            Path dest = uploadsDir.resolve(filename);
            file.transferTo(dest);
            String url = "/api/users/" + id + "/images/" + filename;
            userService.updateCover(id, url);
            return ResponseEntity.ok(Map.of("coverUrl", url));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Upload failed"));
        }
    }

    // GET /api/users/{id}/images/{filename} — serve image file
    @GetMapping("/{id}/images/{filename}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id,
                                           @PathVariable String filename) {
        try {
            Path filePath = uploadsDir.resolve(filename);
            if (!Files.exists(filePath)) return ResponseEntity.notFound().build();
            byte[] bytes = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/octet-stream";
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(bytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf("."));
    }
}
