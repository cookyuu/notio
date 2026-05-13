package com.notio.channel.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.channel.domain.RoutingCondition;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RoutingConditionConverter implements AttributeConverter<RoutingCondition, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(RoutingCondition condition) {
        if (condition == null) {
            return "{}";
        }
        try {
            return MAPPER.writeValueAsString(condition);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @Override
    public RoutingCondition convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return RoutingCondition.empty();
        }
        try {
            return MAPPER.readValue(json, RoutingCondition.class);
        } catch (JsonProcessingException e) {
            return RoutingCondition.empty();
        }
    }
}
