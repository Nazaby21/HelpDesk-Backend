package com.zaby.helpdesk.service.notification;

import com.zaby.helpdesk.model.Notification;
import com.zaby.helpdesk.model.User;

public interface NotificationChannel {
    void send(Notification notification, User recipient);
}
