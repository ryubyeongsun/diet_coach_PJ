package com.dietcoach.project.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {

    @Email @NotBlank
    private String email;

    @NotBlank
    private String name;

    @NotBlank @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다.")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$",
        message = "비밀번호는 영문/숫자/특수문자를 각각 1개 이상 포함해야 합니다."
    )
    private String password;
}
