package com.notio.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String content) {
}

