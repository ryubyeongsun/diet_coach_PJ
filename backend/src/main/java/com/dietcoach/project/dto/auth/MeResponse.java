package com.dietcoach.project.dto.auth;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeResponse {
    private Long id;
    private String email;
    private String name;
}
