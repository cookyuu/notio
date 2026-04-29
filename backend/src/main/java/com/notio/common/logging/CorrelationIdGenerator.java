package com.notio.common.logging;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CorrelationIdGenerator {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
