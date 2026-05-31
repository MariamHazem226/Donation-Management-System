package com.givinghands.givinghands.repository;

import com.givinghands.givinghands.entity.NewsletterSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, Long> {

    Optional<NewsletterSubscriber> findByEmail(String email);

    List<NewsletterSubscriber> findByActiveTrue();
}

