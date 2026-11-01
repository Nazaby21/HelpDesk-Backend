package com.zaby.helpdesk.security;

import com.zaby.helpdesk.model.RefreshToken;
import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresDate(LocalDateTime.now().plusDays(7)); // 7 days expiry
        return refreshTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token).orElse(null);
    }

    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }
}
