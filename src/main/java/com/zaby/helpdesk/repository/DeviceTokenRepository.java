package com.zaby.helpdesk.repository;

import com.zaby.helpdesk.model.DeviceToken;
import com.zaby.helpdesk.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    Optional<DeviceToken> findByDeviceToken(String deviceToken);
    List<DeviceToken> findByUser(User user);
    void deleteByDeviceToken(String deviceToken);
}
