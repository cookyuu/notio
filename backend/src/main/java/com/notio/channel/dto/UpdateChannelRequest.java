package com.notio.channel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateChannelRequest(
    @JsonProperty("display_name") String displayName,
    @JsonProperty("credential_plaintext") String credentialPlaintext,
    @JsonProperty("target_identifier") String targetIdentifier
) {}
