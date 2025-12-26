// com.dietcoach.project.domain.auth.RefreshToken
package com.dietcoach.project.domain.auth;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    private Long id;
    private Long userId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private Boolean revoked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
