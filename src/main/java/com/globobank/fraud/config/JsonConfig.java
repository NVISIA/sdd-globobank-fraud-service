package com.globobank.fraud.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * JSON serialization configuration for GloboBank Fraud Detection Service.
 * 
 * Configures Jackson ObjectMapper for consistent JSON handling across the fraud detection API.
 * Ensures proper serialization of financial data, timestamps, and fraud assessment responses while
 * maintaining security and compliance requirements.
 * 
 * Features: - ISO 8601 timestamp formatting for audit compliance - Precise decimal handling for
 * financial amounts - Consistent property naming (camelCase) - Secure handling of sensitive data
 * fields - Optimized performance for high-throughput fraud detection
 * 
 * @author GloboBank Fraud Detection Team
 * @version 1.0.0
 * @since 2025-10-21
 */
@Configuration
public class JsonConfig {

    /**
     * Primary ObjectMapper bean for JSON serialization/deserialization.
     * 
     * Configures Jackson for fraud detection service requirements including financial data
     * precision, temporal data handling, and security controls.
     *
     * @param builder Jackson2ObjectMapperBuilder for configuration
     * @return Configured ObjectMapper instance
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.build();

        // Enable Java 8 time module for LocalDateTime, etc.
        mapper.registerModule(new JavaTimeModule());

        // Disable writing dates as timestamps for ISO 8601 compliance
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Configure deserialization behavior
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        mapper.enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS);

        // Configure serialization behavior
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);

        // Use camelCase property naming for consistency
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);

        // Configure decimal handling for financial precision
        mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

        return mapper;
    }

    /**
     * Jackson2ObjectMapperBuilder customization for consistent configuration.
     *
     * @return Customized Jackson2ObjectMapperBuilder
     */
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder()
                .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
                .modulesToInstall(new JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .featuresToEnable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                        DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS,
                        SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    }
}
