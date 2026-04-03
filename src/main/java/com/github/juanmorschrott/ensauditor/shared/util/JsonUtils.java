package com.github.juanmorschrott.ensauditor.shared.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utility for JSON serialization and deserialization.
 */
public class JsonUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Serializes an object to JSON string.
     * @param object the object to serialize
     * @return JSON string representation
     */
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    /**
     * Serializes an object to pretty-printed JSON string.
     * @param object the object to serialize
     * @return formatted JSON string
     */
    public static String toJsonPretty(Object object) {
        return toJson(object); // Already configured for pretty printing
    }

    /**
     * Deserializes a JSON string to an object.
     * @param json the JSON string
     * @param valueType the target class
     * @return deserialized object
     */
    public static <T> T fromJson(String json, Class<T> valueType) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize from JSON", e);
        }
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
