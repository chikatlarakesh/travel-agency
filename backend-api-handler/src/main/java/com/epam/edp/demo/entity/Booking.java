package com.epam.edp.demo.entity;

import com.epam.edp.demo.enums.BookingStatus;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "bookings")
@CompoundIndex(name = "agent_state_idx", def = "{'travelAgentId': 1, 'state': 1}")
public class Booking {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String tourId;

    /** Foreign key to the specific TourInstance that was booked. */
    private String tourInstanceId;

    private BookingStatus state;

    private String date;
    private String duration;
    private String mealPlan;
    private GuestCount guests;
    private List<PersonDetail> personalDetails;

    @Indexed
    private String travelAgentId;

    /** @deprecated use {@link #cancellation} instead */
    private String canceledBy;
    /** @deprecated use {@link #cancellation} instead */
    private String cancelReason;

    private Double totalPrice;

    // ── Sprint 2 fields ──────────────────────────────────────────────────────

    /** Documents uploaded by the customer (passports, payment receipts, etc.). */
    private List<BookingDocument> documents = new ArrayList<>();

    /** Confirmation metadata set by the travel agent. */
    private ConfirmationDetails confirmation;

    /** Cancellation metadata. */
    private CancellationDetails cancellation;

    /** Customer approval record for travel-agent proposed edits (offline). */
    private CustomerApproval customerApproval;

    /** Snapshot of original values taken before an agent edit; used to revert on customer decline. */
    private BookingEditSnapshot editSnapshot;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Optimistic-locking version field.
     * MongoDB increments this on every update; concurrent writes to the same
     * document will fail with {@code OptimisticLockingFailureException}.
     */
    @Version
    private Long version;
}
