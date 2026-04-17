package com.notio.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshRequest {

    @NotBlank(message = "Refresh token은 필수입니다.")
    @Size(max = 4096, message = "Refresh token은 최대 4096자까지 가능합니다.")
    private String refreshToken;
}
