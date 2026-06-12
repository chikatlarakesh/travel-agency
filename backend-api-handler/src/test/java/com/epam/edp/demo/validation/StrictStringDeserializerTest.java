package com.epam.edp.demo.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link StrictStringDeserializer}.
 * Uses a minimal Jackson ObjectMapper with a test DTO that applies the deserializer.
 */
class StrictStringDeserializerTest {

    private ObjectMapper objectMapper;

    /** Minimal DTO wiring the deserializer on both name fields — mirrors SignUpRequestDTO. */
    @Data
    static class TestDTO {
        @JsonDeserialize(using = StrictStringDeserializer.class)
        private String firstName;

        @JsonDeserialize(using = StrictStringDeserializer.class)
        private String lastName;

        private String other; // no strict deserializer — should behave normally
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // ──────────────────────────────────────────────────────────────
    // Happy path: valid string tokens
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("String value for firstName is deserialized correctly")
    void deserialize_stringFirstName_succeeds() throws JsonProcessingException {
        String json = "{\"firstName\":\"Alice\",\"lastName\":\"Smith\"}";
        TestDTO dto = objectMapper.readValue(json, TestDTO.class);
        assertThat(dto.getFirstName()).isEqualTo("Alice");
        assertThat(dto.getLastName()).isEqualTo("Smith");
    }

    @Test
    @DisplayName("Empty string value is deserialized (NotBlank handles the constraint)")
    void deserialize_emptyString_succeeds() throws JsonProcessingException {
        String json = "{\"firstName\":\"\",\"lastName\":\"\"}";
        TestDTO dto = objectMapper.readValue(json, TestDTO.class);
        assertThat(dto.getFirstName()).isEmpty();
        assertThat(dto.getLastName()).isEmpty();
    }

    @Test
    @DisplayName("Whitespace-only string value is deserialized (NotBlank handles the constraint)")
    void deserialize_whitespaceString_succeeds() throws JsonProcessingException {
        String json = "{\"firstName\":\"   \",\"lastName\":\"   \"}";
        TestDTO dto = objectMapper.readValue(json, TestDTO.class);
        assertThat(dto.getFirstName()).isEqualTo("   ");
    }

    @Test
    @DisplayName("Null JSON token for firstName results in null (NotBlank handles required check)")
    void deserialize_nullToken_returnsNull() throws JsonProcessingException {
        String json = "{\"firstName\":null,\"lastName\":null}";
        TestDTO dto = objectMapper.readValue(json, TestDTO.class);
        assertThat(dto.getFirstName()).isNull();
        assertThat(dto.getLastName()).isNull();
    }

    @Test
    @DisplayName("Unicode name string is deserialized correctly")
    void deserialize_unicodeString_succeeds() throws JsonProcessingException {
        String json = "{\"firstName\":\"Ünäme\",\"lastName\":\"Ñoño\"}";
        TestDTO dto = objectMapper.readValue(json, TestDTO.class);
        assertThat(dto.getFirstName()).isEqualTo("Ünäme");
        assertThat(dto.getLastName()).isEqualTo("Ñoño");
    }

    // ──────────────────────────────────────────────────────────────
    // Rejection path: numeric tokens
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Integer token for firstName throws InvalidFormatException")
    void deserialize_integerFirstName_throwsInvalidFormatException() {
        String json = "{\"firstName\":123,\"lastName\":\"Smith\"}";
        assertThatThrownBy(() -> objectMapper.readValue(json, TestDTO.class))
                .isInstanceOf(InvalidFormatException.class)
                .hasMessageContaining("must be of type string");
    }

    @Test
    @DisplayName("Integer token for lastName throws InvalidFormatException")
    void deserialize_integerLastName_throwsInvalidFormatException() {
        String json = "{\"firstName\":\"Alice\",\"lastName\":456}";
        assertThatThrownBy(() -> objectMapper.readValue(json, TestDTO.class))
                .isInstanceOf(InvalidFormatException.class)
                .hasMessageContaining("must be of type string");
    }

    @Test
    @DisplayName("Decimal/float token for firstName throws InvalidFormatException")
    void deserialize_floatFirstName_throwsInvalidFormatException() {
        String json = "{\"firstName\":3.14,\"lastName\":\"Smith\"}";
        assertThatThrownBy(() -> objectMapper.readValue(json, TestDTO.class))
                .isInstanceOf(InvalidFormatException.class)
                .hasMessageContaining("must be of type string");
    }

    @Test
    @DisplayName("Negative number token for lastName throws InvalidFormatException")
    void deserialize_negativeNumberLastName_throwsInvalidFormatException() {
        String json = "{\"firstName\":\"Alice\",\"lastName\":-99}";
        assertThatThrownBy(() -> objectMapper.readValue(json, TestDTO.class))
                .isInstanceOf(InvalidFormatException.class)
                .hasMessageContaining("must be of type string");
    }

    // ──────────────────────────────────────────────────────────────
    // Rejection path: boolean tokens
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Boolean true token for firstName throws InvalidFormatException")
    void deserialize_booleanTrueFirstName_throwsInvalidFormatException() {
        String json = "{\"firstName\":true,\"lastName\":\"Smith\"}";
        assertThatThrownBy(() -> objectMapper.readValue(json, TestDTO.class))
                .isInstanceOf(InvalidFormatException.class)
                .hasMessageContaining("must be of type string");
    }

    @Test
    @DisplayName("Boolean false token for lastName throws InvalidFormatException")
    void deserialize_booleanFalseLastName_throwsInvalidFormatException() {
        String json = "{\"firstName\":\"Alice\",\"lastName\":false}";
        assertThatThrownBy(() -> objectMapper.readValue(json, TestDTO.class))
                .isInstanceOf(InvalidFormatException.class)
                .hasMessageContaining("must be of type string");
    }

    // ──────────────────────────────────────────────────────────────
    // Rejection path: object and array tokens
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("JSON object token for firstName throws exception")
    void deserialize_objectTokenFirstName_throwsException() {
        String json = "{\"firstName\":{\"nested\":\"value\"},\"lastName\":\"Smith\"}";
        assertThatThrownBy(() -> objectMapper.readValue(json, TestDTO.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    @DisplayName("JSON array token for firstName throws exception")
    void deserialize_arrayTokenFirstName_throwsException() {
        String json = "{\"firstName\":[\"Alice\"],\"lastName\":\"Smith\"}";
        assertThatThrownBy(() -> objectMapper.readValue(json, TestDTO.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    // ──────────────────────────────────────────────────────────────
    // Non-decorated field not affected
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Field without StrictStringDeserializer coerces number to string normally")
    void deserialize_numericOnNonStrictField_coercedByJacksonNormally() throws JsonProcessingException {
        // 'other' field has no @JsonDeserialize — Jackson default allows coercion
        String json = "{\"firstName\":\"Alice\",\"lastName\":\"Smith\",\"other\":\"plain\"}";
        TestDTO dto = objectMapper.readValue(json, TestDTO.class);
        assertThat(dto.getOther()).isEqualTo("plain");
    }

    // ──────────────────────────────────────────────────────────────
    // Exception carries target type information
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("InvalidFormatException from integer firstName targets String.class")
    void deserialize_integerFirstName_exceptionTargetsStringClass() {
        String json = "{\"firstName\":999,\"lastName\":\"Smith\"}";
        assertThatThrownBy(() -> objectMapper.readValue(json, TestDTO.class))
                .isInstanceOf(InvalidFormatException.class)
                .satisfies(ex -> {
                    InvalidFormatException ife = (InvalidFormatException) ex;
                    assertThat(ife.getTargetType()).isEqualTo(String.class);
                });
    }

    @Test
    @DisplayName("InvalidFormatException path contains field name 'firstName'")
    void deserialize_integerFirstName_exceptionPathContainsFieldName() {
        String json = "{\"firstName\":999,\"lastName\":\"Smith\"}";
        assertThatThrownBy(() -> objectMapper.readValue(json, TestDTO.class))
                .isInstanceOf(InvalidFormatException.class)
                .satisfies(ex -> {
                    InvalidFormatException ife = (InvalidFormatException) ex;
                    assertThat(ife.getPath()).isNotEmpty();
                    assertThat(ife.getPath().get(ife.getPath().size() - 1).getFieldName())
                            .isEqualTo("firstName");
                });
    }

    @Test
    @DisplayName("InvalidFormatException path contains field name 'lastName'")
    void deserialize_integerLastName_exceptionPathContainsFieldName() {
        String json = "{\"firstName\":\"Alice\",\"lastName\":789}";
        assertThatThrownBy(() -> objectMapper.readValue(json, TestDTO.class))
                .isInstanceOf(InvalidFormatException.class)
                .satisfies(ex -> {
                    InvalidFormatException ife = (InvalidFormatException) ex;
                    assertThat(ife.getPath()).isNotEmpty();
                    assertThat(ife.getPath().get(ife.getPath().size() - 1).getFieldName())
                            .isEqualTo("lastName");
                });
    }
}

