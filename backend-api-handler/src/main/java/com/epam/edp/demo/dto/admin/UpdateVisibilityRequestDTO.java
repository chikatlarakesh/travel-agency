package com.epam.edp.demo.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateVisibilityRequestDTO {

    @NotBlank
    @Pattern(regexp = "PUBLISHED|HIDDEN", message = "visibility must be PUBLISHED or HIDDEN")
    private String visibility;
}
