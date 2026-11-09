package com.zaby.helpdesk.service.notification;

import com.zaby.helpdesk.model.Notification;
import com.zaby.helpdesk.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(4)
public class EmailNotificationChannel implements NotificationChannel {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${notifications.email.enabled:false}")
    private boolean emailEnabled = false;

    @Value("${notifications.email.from:noreply@helpdesk.local}")
    private String fromAddress = "noreply@helpdesk.local";

    public EmailNotificationChannel(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    @Override
    public void send(Notification notification, User recipient) {
        if (!emailEnabled) {
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            return;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setTo(recipient.getEmail());
            mail.setSubject(notification.getTitle());
            mail.setText(notification.getMessage());
            mailSender.send(mail);
            log.debug("Sent email notification to {}", recipient.getEmail());
        } catch (Exception ex) {
            log.error("Failed to send email notification to {}", recipient.getEmail(), ex);
        }
    }
}
