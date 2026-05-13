package com.notio.channel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.notio.channel.domain.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateChannelRequest(
    @NotBlank @JsonProperty("display_name") String displayName,
    @NotNull @JsonProperty("channel_type") ChannelType channelType,
    @NotBlank @JsonProperty("credential_plaintext") String credentialPlaintext,
    @JsonProperty("target_identifier") String targetIdentifier
) {}
