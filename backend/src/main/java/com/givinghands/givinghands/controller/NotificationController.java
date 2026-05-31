package com.givinghands.givinghands.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    // GET /api/notifications/user/{userId}
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<String>> getUserNotifications(@PathVariable Long userId) {
        // In-app notifications will be implemented in a future layer.
        return ResponseEntity.ok(Collections.emptyList());
    }
}
