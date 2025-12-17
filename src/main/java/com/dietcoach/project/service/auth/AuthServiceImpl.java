package com.dietcoach.project.service.auth;

import com.dietcoach.project.common.error.BusinessException;
import com.dietcoach.project.domain.User;
import com.dietcoach.project.domain.auth.RefreshToken;
import com.dietcoach.project.dto.UserProfileResponse;
import com.dietcoach.project.dto.auth.AuthResponse;
import com.dietcoach.project.dto.auth.LoginRequest;
import com.dietcoach.project.dto.auth.SignupRequest;
import com.dietcoach.project.mapper.UserMapper;
import com.dietcoach.project.mapper.auth.RefreshTokenMapper;
import com.dietcoach.project.security.JwtTokenProvider;
import com.dietcoach.project.security.TokenHasher;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenMapper refreshTokenMapper;

    @Override
    @Transactional
    public Long signup(SignupRequest request) {
        User exists = userMapper.findByEmail(request.getEmail());
        if (exists != null) {
            throw new BusinessException("이미 가입된 이메일입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .build();

        userMapper.insertUser(user);
        return user.getId();
    }

    @Override
    @Transactional // ✅ readOnly=false 로 저장 가능하게
    public AuthResponse login(LoginRequest request) {
        User user = userMapper.findByEmail(request.getEmail());
        if (user == null) {
            throw new BusinessException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // ✅ access/refresh 둘 다 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // ✅ refresh token DB 저장(해시)
        LocalDateTime now = LocalDateTime.now();
        RefreshToken row = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(TokenHasher.sha256(refreshToken))
                .expiresAt(jwtTokenProvider.getExpiry(refreshToken))
                .revoked(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
        refreshTokenMapper.insert(row);

        // ✅ 응답에 refreshToken 포함
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse me(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) throw new BusinessException("존재하지 않는 사용자입니다.");
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .height(user.getHeight())
                .weight(user.getWeight())
                .activityLevel(user.getActivityLevel())
                .goalType(user.getGoalType())
                .bmr(user.getBmr())
                .tdee(user.getTdee())
                .targetCalories(user.getTargetCalories())
                .build();
    }
    @Override
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BusinessException("Refresh token is required");
        }

        // (중요) 혹시 "Bearer "가 붙어서 들어오면 제거
        String token = stripBearer(rawRefreshToken);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new BusinessException("Invalid refresh token");
        }
        if (!jwtTokenProvider.isRefreshToken(token)) {
            throw new BusinessException("Token type is not refresh");
        }

        String tokenHash = TokenHasher.sha256(token);

        RefreshToken saved = refreshTokenMapper.findByTokenHash(tokenHash);
        if (saved == null) {
            throw new BusinessException("Refresh token not found");
        }
        if (Boolean.TRUE.equals(saved.getRevoked())) {
            throw new BusinessException("Refresh token revoked");
        }
        if (saved.getExpiresAt() != null && saved.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Refresh token expired");
        }

        Long userId = saved.getUserId();

        // ✅ 로테이션: 기존 토큰 폐기
        refreshTokenMapper.revokeById(saved.getId());

        // ✅ 새 토큰 발급
        String newAccess = jwtTokenProvider.createAccessToken(userId);
        String newRefresh = jwtTokenProvider.createRefreshToken(userId);

        // ✅ 새 refresh를 DB에 저장(해시)
        RefreshToken newRow = RefreshToken.builder()
                .userId(userId)
                .tokenHash(TokenHasher.sha256(newRefresh))
                .expiresAt(jwtTokenProvider.getExpiry(newRefresh))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        refreshTokenMapper.insert(newRow);

        return AuthResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .tokenType("Bearer")
                .build();
    }

    private String stripBearer(String v) {
        String s = v.trim();
        if (s.toLowerCase().startsWith("bearer ")) {
            return s.substring(7).trim();
        }
        return s;
    }
}