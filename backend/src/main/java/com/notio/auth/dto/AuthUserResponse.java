package com.notio.auth.dto;

import com.notio.auth.domain.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserResponse {

    private Long id;
    private String primaryEmail;
    private String displayName;
    private UserStatus status;
}
