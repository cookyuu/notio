package com.notio.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutResponse {

    private String message;

    public static LogoutResponse success() {
        return LogoutResponse.builder()
                .message("로그아웃되었습니다.")
                .build();
    }
}
