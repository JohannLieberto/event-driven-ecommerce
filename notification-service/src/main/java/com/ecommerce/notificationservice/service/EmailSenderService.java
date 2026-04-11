package com.ecommerce.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {

    private static final Logger log = LoggerFactory.getLogger(EmailSenderService.class);

    public void sendEmail(String email, String subject, String body) {
        log.info("[EMAIL] Sending email to={} subject='{}' body='{}'", email, subject, body);
    }
}