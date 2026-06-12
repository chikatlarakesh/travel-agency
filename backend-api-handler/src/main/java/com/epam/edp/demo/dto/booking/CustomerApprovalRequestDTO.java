package com.epam.edp.demo.dto.booking;

import com.epam.edp.demo.enums.ApprovalMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer approval for travel agent proposed booking changes")
public class CustomerApprovalRequestDTO {

    @NotNull(message = "approvalMode is required")
    @Schema(description = "How the approval was obtained", example = "OFFLINE", required = true)
    private ApprovalMode approvalMode;

    @NotNull(message = "approvalGiven is required")
    @Schema(description = "Whether the customer approved the changes", example = "true", required = true)
    private Boolean approvalGiven;

    @Schema(description = "Optional customer note about the approval decision")
    private String approvalNote;
}
