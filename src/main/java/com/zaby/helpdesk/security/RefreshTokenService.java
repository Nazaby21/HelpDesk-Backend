package com.zaby.helpdesk.security;

import com.zaby.helpdesk.model.RefreshToken;
import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshToken createRefreshToken(User user){

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresDate(LocalDateTime.now().plusDays(7));

        return refreshTokenRepository.save(token);
    }

    public RefreshToken verifyToken(String token){

        RefreshToken refreshToken =
                refreshTokenRepository.findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException("Token not found"));

        if(refreshToken.getExpiresDate().isBefore(LocalDateTime.now())){
            throw new RuntimeException("Token expired");
        }

        return refreshToken;
    }
}
