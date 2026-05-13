package com.notio.channel.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;

@Converter
public class LongListConverter implements AttributeConverter<List<Long>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<Long>> TYPE_REF = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<Long> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @Override
    public List<Long> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(json, TYPE_REF);
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
