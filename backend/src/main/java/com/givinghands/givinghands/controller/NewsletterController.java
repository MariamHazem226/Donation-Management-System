package com.givinghands.givinghands.controller;

import com.givinghands.givinghands.dto.RegisterRequestDTO;
import com.givinghands.givinghands.entity.NewsletterSubscriber;
import com.givinghands.givinghands.service.NewsletterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/newsletter")
@CrossOrigin(origins = "*")
public class NewsletterController {

    private final NewsletterService newsletterService;

    public NewsletterController(NewsletterService newsletterService) {
        this.newsletterService = newsletterService;
    }

    // POST /api/newsletter/subscribe
    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String name = body.get("name");

        NewsletterSubscriber subscriber = newsletterService.subscribe(email, name);
        return ResponseEntity.ok(Map.of(
                "message", "Subscribed successfully",
                "email", subscriber.getEmail(),
                "id", subscriber.getId()
        ));
    }
}

