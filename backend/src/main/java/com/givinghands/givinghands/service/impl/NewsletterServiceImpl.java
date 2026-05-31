package com.givinghands.givinghands.service.impl;

import com.givinghands.givinghands.entity.NewsletterSubscriber;
import com.givinghands.givinghands.repository.NewsletterSubscriberRepository;
import com.givinghands.givinghands.service.NewsletterService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NewsletterServiceImpl implements NewsletterService {

    private final NewsletterSubscriberRepository repository;

    public NewsletterServiceImpl(NewsletterSubscriberRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<NewsletterSubscriber> getByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Override
    public NewsletterSubscriber subscribe(String email, String name) {
        String normalized = email != null ? email.trim().toLowerCase() : null;
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }

        return repository.findByEmail(normalized)
                .map(existing -> {
                    existing.setActive(true);
                    if (name != null && !name.isBlank()) existing.setName(name.trim());
                    return repository.save(existing);
                })
                .orElseGet(() -> {
                    NewsletterSubscriber sub = new NewsletterSubscriber();
                    sub.setEmail(normalized);
                    sub.setName(name);
                    sub.setActive(true);
                    return repository.save(sub);
                });
    }

    @Override
    public List<NewsletterSubscriber> getActiveSubscribers() {
        return repository.findByActiveTrue();
    }
}

