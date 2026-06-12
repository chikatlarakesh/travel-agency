package com.epam.edp.demo.validation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;

/**
 * Rejects non-string JSON tokens for fields that are expected to be text.
 */
public class StrictStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_STRING) {
            return parser.getValueAsString();
        }
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }

        Object receivedValue = parser.getEmbeddedObject();
        if (receivedValue == null) {
            receivedValue = parser.getText();
        }

        throw InvalidFormatException.from(parser, "must be of type string", receivedValue, String.class);
    }
}

