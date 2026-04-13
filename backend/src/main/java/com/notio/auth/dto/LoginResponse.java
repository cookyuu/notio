package com.notio.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String userId;
    private String email;
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn; // seconds
}
