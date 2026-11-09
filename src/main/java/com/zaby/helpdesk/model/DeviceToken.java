package com.zaby.helpdesk.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_token", nullable = false, length = 512, unique = true)
    private String token;

    @Column(name = "platform", length = 50)
    private String platform;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public String getDeviceToken() {
        return token;
    }

    public void setDeviceToken(String deviceToken) {
        this.token = deviceToken;
    }

    public static class DeviceTokenBuilder {
        public DeviceTokenBuilder deviceToken(String deviceToken) {
            this.token = deviceToken;
            return this;
        }
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.lastUsedAt == null) {
            this.lastUsedAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }
}
