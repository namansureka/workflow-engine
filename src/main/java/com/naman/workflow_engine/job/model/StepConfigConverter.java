package com.naman.workflow_engine.job.model;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

@Converter
public class StepConfigConverter implements AttributeConverter<List<StepConfig>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<StepConfig> steps) {
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (Exception e) {
            throw new RuntimeException("Error converting steps to JSON", e);
        }
    }

    @Override
    public List<StepConfig> convertToEntityAttribute(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<StepConfig>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Error converting JSON to steps", e);
        }
    }
}