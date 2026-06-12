package com.epam.edp.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String error;
    private List<String> details;

    public static ErrorResponse of(String error) {
        return new ErrorResponse(error, null);
    }

    public static ErrorResponse of(String error, List<String> details) {
        return new ErrorResponse(error, details);
    }
}
