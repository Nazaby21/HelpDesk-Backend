package com.zaby.helpdesk.repository;

import com.zaby.helpdesk.model.DeviceToken;
import com.zaby.helpdesk.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    default Optional<DeviceToken> findByDeviceToken(String deviceToken) {
        return findByToken(deviceToken);
    }

    List<DeviceToken> findByUser(User user);

    List<DeviceToken> findByUser_Id(Long userId);

    void deleteByToken(String token);

    default void deleteByDeviceToken(String deviceToken) {
        deleteByToken(deviceToken);
    }
}
