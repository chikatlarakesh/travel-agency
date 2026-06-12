package com.epam.edp.demo.dto.booking;

import com.epam.edp.demo.enums.ApprovalMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer approval record for travel-agent proposed edits")
public class CustomerApprovalDTO {
    private ApprovalMode approvalMode;
    private boolean approvalGiven;
    private Instant approvalDate;
    private String approvalNote;
}
