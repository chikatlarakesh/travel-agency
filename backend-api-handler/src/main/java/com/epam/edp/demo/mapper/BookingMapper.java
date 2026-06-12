package com.epam.edp.demo.mapper;

import com.epam.edp.demo.dto.booking.BookedTourDTO;
import com.epam.edp.demo.dto.booking.BookingDocumentDTO;
import com.epam.edp.demo.dto.booking.CancellationDTO;
import com.epam.edp.demo.dto.booking.ConfirmationDTO;
import com.epam.edp.demo.dto.booking.CustomerApprovalDTO;
import com.epam.edp.demo.dto.booking.CustomerDetailsDTO;
import com.epam.edp.demo.dto.booking.PersonDetailDTO;
import com.epam.edp.demo.dto.booking.TourDetailsBookingDTO;
import com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO;
import com.epam.edp.demo.dto.booking.TravelAgentDTO;
import com.epam.edp.demo.entity.Booking;
import com.epam.edp.demo.entity.BookingDocument;
import com.epam.edp.demo.entity.CancellationDetails;
import com.epam.edp.demo.entity.ConfirmationDetails;
import com.epam.edp.demo.entity.CustomerApproval;
import com.epam.edp.demo.entity.Destination;
import com.epam.edp.demo.entity.PersonDetail;
import com.epam.edp.demo.entity.PricingOption;
import com.epam.edp.demo.entity.Tour;
import com.epam.edp.demo.entity.TourInstance;
import com.epam.edp.demo.entity.TravelAgent;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.util.MealPlanFormatter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class BookingMapper {

    private final MealPlanFormatter mealPlanFormatter;

    public BookingMapper(MealPlanFormatter mealPlanFormatter) {
        this.mealPlanFormatter = mealPlanFormatter;
    }

    // =========================================================================
    // Sprint 1 mapper
    // =========================================================================

    public BookedTourDTO toBookedTourDTO(Booking booking, Tour tour,
                                         List<TourInstance> instances, TravelAgent agent) {
        BookedTourDTO dto = new BookedTourDTO();
        dto.setId(booking.getId());
        dto.setTourId(booking.getTourId());
        dto.setState(booking.getState() != null ? booking.getState().name() : null);
        dto.setTourImageUrl(tour.getImageUrls() != null && !tour.getImageUrls().isEmpty()
                ? tour.getImageUrls().get(0) : null);
        dto.setName(tour.getName());
        dto.setDestination(formatDestination(tour.getDestination()));
        dto.setTourDetails(buildTourDetails(booking, instances));
        dto.setTravelAgent(buildTravelAgentDTO(agent));
        dto.setCanceledBy(booking.getCanceledBy());
        dto.setCancelReason(booking.getCancelReason());

        if (booking.getPersonalDetails() != null && !booking.getPersonalDetails().isEmpty()) {
            List<PersonDetailDTO> personalDetailDTOs = booking.getPersonalDetails().stream()
                    .map(p -> {
                        PersonDetailDTO pdto = new PersonDetailDTO();
                        pdto.setFirstName(p.getFirstName());
                        pdto.setLastName(p.getLastName());
                        return pdto;
                    })
                    .toList();
            dto.setPersonalDetails(personalDetailDTOs);
        }
        if (booking.getGuests() != null) {
            dto.setTravelerCount(booking.getGuests().getAdult());
            dto.setChildrenCount(booking.getGuests().getChildren());
        }

        if (booking.getDate() != null && tour.getFreeCancellationDays() > 0) {
            LocalDate tourStartDate = LocalDate.parse(booking.getDate());
            LocalDate freeCancellationDate = tourStartDate.minusDays(tour.getFreeCancellationDays());
            dto.setFreeCancellationDate(freeCancellationDate.toString());
        }

        // Sprint 2 additions
        dto.setDocuments(mapDocuments(booking.getDocuments()));
        dto.setConfirmation(mapConfirmation(booking.getConfirmation()));
        dto.setCancellationDetails(mapCancellation(booking.getCancellation()));
        dto.setCustomerApproval(mapCustomerApproval(booking.getCustomerApproval()));

        return dto;
    }

    // =========================================================================
    // Sprint 2 mapper — agent-oriented detail view
    // =========================================================================

    public TravelAgentBookingDetailDTO toAgentDetailDTO(Booking booking, Tour tour, User customer,
                                                          List<TourInstance> instances) {
        // Resolve total price: use stored value if present, else compute from instance pricing
        Double resolvedPrice = booking.getTotalPrice();
        if ((resolvedPrice == null || resolvedPrice == 0) && instances != null && booking.getGuests() != null) {
            String computed = computeTotalPrice(booking, instances);
            if (computed != null && !computed.equals("$0")) {
                try {
                    resolvedPrice = Double.parseDouble(computed.replace("$", "").replace(",", ""));
                } catch (NumberFormatException ignored) { /* keep null */ }
            }
        }

        TravelAgentBookingDetailDTO dto = TravelAgentBookingDetailDTO.builder()
                .bookingId(booking.getId())
                .state(booking.getState() != null ? booking.getState().name() : null)
                .tourName(tour != null ? tour.getName() : null)
                .destination(tour != null ? formatDestination(tour.getDestination()) : null)
                .tourImageUrl(tour != null && tour.getImageUrls() != null && !tour.getImageUrls().isEmpty()
                        ? tour.getImageUrls().get(0) : null)
                .tourRating(tour != null ? tour.getRating() : null)
                .startDate(booking.getDate())
                .duration(booking.getDuration())
                .mealPlan(booking.getMealPlan())
                .totalPrice(resolvedPrice)
                .documents(mapDocuments(booking.getDocuments()))
                .documentCount(booking.getDocuments() != null ? booking.getDocuments().size() : 0)
                .verifiedDocumentCount(booking.getDocuments() != null
                        ? (int) booking.getDocuments().stream().filter(BookingDocument::isVerified).count()
                        : 0)
                .confirmation(mapConfirmation(booking.getConfirmation()))
                .cancellation(mapCancellation(booking.getCancellation()))
                .customerApproval(mapCustomerApproval(booking.getCustomerApproval()))
                .build();

        // Customer details
        if (customer != null) {
            int adults = booking.getGuests() != null ? booking.getGuests().getAdult() : 0;
            int children = booking.getGuests() != null ? booking.getGuests().getChildren() : 0;
            dto.setCustomerDetails(CustomerDetailsDTO.builder()
                    .firstName(customer.getFirstName())
                    .lastName(customer.getLastName())
                    .email(customer.getEmail())
                    .guestCount(adults + children)
                    .adults(adults)
                    .children(children)
                    .build());
        }

        // Guest detail list
        if (booking.getPersonalDetails() != null) {
            dto.setGuestDetails(booking.getPersonalDetails().stream()
                    .map(p -> {
                        PersonDetailDTO pdto = new PersonDetailDTO();
                        pdto.setFirstName(p.getFirstName());
                        pdto.setLastName(p.getLastName());
                        return pdto;
                    })
                    .toList());
        }

        // Free cancellation date
        if (tour != null && booking.getDate() != null && tour.getFreeCancellationDays() > 0) {
            LocalDate startDate = LocalDate.parse(booking.getDate());
            dto.setFreeCancellationDate(
                    startDate.minusDays(tour.getFreeCancellationDays()).toString());
        }

        return dto;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private List<BookingDocumentDTO> mapDocuments(List<BookingDocument> docs) {
        if (docs == null) {
            return Collections.emptyList();
        }
        return docs.stream().map(d -> BookingDocumentDTO.builder()
                .id(d.getId())
                .type(d.getType())
                .fileName(d.getFileName())
                .fileUrl(d.getFileUrl())
                .verified(d.isVerified())
                .verifiedAt(d.getVerifiedAt())
                .verifiedBy(d.getVerifiedBy())
                .uploadedAt(d.getUploadedAt())
                .build()).toList();
    }

    private ConfirmationDTO mapConfirmation(ConfirmationDetails c) {
        if (c == null) return null;
        return ConfirmationDTO.builder()
                .confirmedBy(c.getConfirmedBy())
                .confirmedAt(c.getConfirmedAt())
                .notes(c.getNotes())
                .build();
    }

    private CancellationDTO mapCancellation(CancellationDetails c) {
        if (c == null) return null;
        return CancellationDTO.builder()
                .reason(c.getReason())
                .reasonNote(c.getReasonNote())
                .canceledBy(c.getCanceledBy())
                .canceledAt(c.getCanceledAt())
                .refundEligible(c.isRefundEligible())
                .build();
    }

    private CustomerApprovalDTO mapCustomerApproval(CustomerApproval a) {
        if (a == null) return null;
        return CustomerApprovalDTO.builder()
                .approvalMode(a.getApprovalMode())
                .approvalGiven(a.isApprovalGiven())
                .approvalDate(a.getApprovalDate())
                .approvalNote(a.getApprovalNote())
                .build();
    }

    private TourDetailsBookingDTO buildTourDetails(Booking booking,
                                                    List<TourInstance> instances) {
        String formattedDate = formatBookingDate(booking.getDate(), booking.getDuration());
        String formattedMealPlan = mealPlanFormatter.format(booking.getMealPlan());
        String guestsText = buildGuestsText(booking);
        String totalPrice;

        if (booking.getTotalPrice() != null && booking.getTotalPrice() > 0) {
            totalPrice = "$" + booking.getTotalPrice().longValue();
        } else {
            totalPrice = computeTotalPrice(booking, instances);
        }

        int docCount = booking.getDocuments() != null ? booking.getDocuments().size() : 0;
        String documents = docCount + " item" + (docCount != 1 ? "s" : "");

        return new TourDetailsBookingDTO(formattedDate, formattedMealPlan, guestsText, totalPrice, documents);
    }

    private String formatDestination(Destination destination) {
        if (destination == null) {
            return "";
        }
        return destination.getCity() + ", " + destination.getCountry();
    }

    private String computeTotalPrice(Booking booking, List<TourInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return "$0";
        }

        TourInstance target = instances.stream()
                .filter(i -> i.getStartDate() != null
                        && i.getStartDate().toString().equals(booking.getDate()))
                .findFirst()
                .orElse(instances.get(0));

        if (target.getPricing() == null) {
            return "$0";
        }

        int durationDays = parseDurationDays(booking.getDuration());
        BigDecimal pricePerPerson = target.getPricing().stream()
                .filter(p -> p.getDuration() == durationDays)
                .map(PricingOption::getPricePerPerson)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (pricePerPerson == null) {
            return "$0";
        }

        int totalGuests = booking.getGuests().getAdult() + booking.getGuests().getChildren();
        BigDecimal total = pricePerPerson.multiply(BigDecimal.valueOf(totalGuests));
        return "$" + total.stripTrailingZeros().toPlainString();
    }

    private String formatBookingDate(String dateStr, String duration) {
        LocalDate date = LocalDate.parse(dateStr);
        String monthDay = date.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH));
        String durDisplay = duration.matches("\\d+") ? duration + " days" : duration;
        return monthDay + ", " + date.getYear() + " (" + durDisplay + ")";
    }

    private String buildGuestsText(Booking booking) {
        PersonDetail first = booking.getPersonalDetails().get(0);
        String name = first.getFirstName() + " " + first.getLastName();
        int adults = booking.getGuests().getAdult();
        int children = booking.getGuests().getChildren();

        StringBuilder sb = new StringBuilder(name).append(" (").append(adults).append(" adult");
        if (adults != 1) {
            sb.append("s");
        }
        if (children > 0) {
            sb.append(", ").append(children).append(" child").append(children != 1 ? "ren" : "");
        }
        sb.append(")");
        return sb.toString();
    }

    private TravelAgentDTO buildTravelAgentDTO(TravelAgent agent) {
        return new TravelAgentDTO(agent.getName(), agent.getEmail(), agent.getPhone(), agent.getMessenger());
    }

    /** "7 days" → 7, "10 days" → 10. Returns 0 on parse failure. */
    private int parseDurationDays(String duration) {
        try {
            return Integer.parseInt(duration.trim().split("\\s+")[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

