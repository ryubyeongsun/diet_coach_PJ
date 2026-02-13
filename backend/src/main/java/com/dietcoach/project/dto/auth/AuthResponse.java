package com.dietcoach.project.dto.auth;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType; // "Bearer"
    private String refreshToken;
}
