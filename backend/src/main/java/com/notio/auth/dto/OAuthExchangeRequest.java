package com.notio.auth.dto;

import com.notio.auth.domain.AuthPlatform;
import com.notio.auth.domain.AuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthExchangeRequest {

    @NotNull(message = "인증 제공자는 필수입니다.")
    private AuthProvider provider;

    @NotNull(message = "인증 플랫폼은 필수입니다.")
    private AuthPlatform platform;

    @NotBlank(message = "state는 필수입니다.")
    @Size(max = 255, message = "state는 최대 255자까지 가능합니다.")
    private String state;

    @NotBlank(message = "인가 코드는 필수입니다.")
    @Size(max = 2048, message = "인가 코드는 최대 2048자까지 가능합니다.")
    private String code;

    @NotBlank(message = "리다이렉트 URI는 필수입니다.")
    @Size(max = 2048, message = "리다이렉트 URI는 최대 2048자까지 가능합니다.")
    private String redirectUri;
}
