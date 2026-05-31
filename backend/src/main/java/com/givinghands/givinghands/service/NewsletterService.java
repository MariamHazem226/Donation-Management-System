package com.givinghands.givinghands.service;

import com.givinghands.givinghands.entity.NewsletterSubscriber;

import java.util.List;
import java.util.Optional;

public interface NewsletterService {

    Optional<NewsletterSubscriber> getByEmail(String email);

    NewsletterSubscriber subscribe(String email, String name);

    List<NewsletterSubscriber> getActiveSubscribers();
}

