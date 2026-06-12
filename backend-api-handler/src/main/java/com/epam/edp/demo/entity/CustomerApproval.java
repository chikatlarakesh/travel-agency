package com.epam.edp.demo.entity;

import com.epam.edp.demo.enums.ApprovalMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerApproval {

    /** How the approval was obtained (currently only OFFLINE supported). */
    private ApprovalMode approvalMode;

    /** Whether the customer gave approval for the travel agent's proposed edit. */
    @Builder.Default
    private boolean approvalGiven = false;

    /** Timestamp when the customer gave (or denied) approval. */
    private Instant approvalDate;

    /** Optional note from the customer regarding the approval decision. */
    private String approvalNote;
}
