package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.tour.DestinationListResponseDTO;
import com.epam.edp.demo.dto.tour.ReviewDTO;
import com.epam.edp.demo.dto.tour.ReviewListResponseDTO;
import com.epam.edp.demo.dto.tour.TourDetailsDTO;
import com.epam.edp.demo.dto.tour.TourFeedbackRequestDTO;
import com.epam.edp.demo.dto.tour.TourListResponseDTO;
import com.epam.edp.demo.dto.user.MessageResponseDTO;
import com.epam.edp.demo.service.TourService;
import com.epam.edp.demo.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/tours")
@RequiredArgsConstructor
@Tag(name = "Tours", description = "Tour browsing and search")
public class TourController {

    private final TourService tourService;

    @Operation(summary = "Autocomplete destination names")
    @ApiResponse(responseCode = "200", description = "Destinations matching query")
    @GetMapping("/destinations")
    public ResponseEntity<DestinationListResponseDTO> getDestinations(
            @Parameter(description = "Search query (minimum 3 characters)")
            @RequestParam String destination) {
        return ResponseEntity.ok(tourService.getDestinations(destination));
    }

    @Operation(summary = "Get paginated list of available tours with filters")
    @ApiResponse(responseCode = "200", description = "Filtered tour list")
    @GetMapping("/available")
    public ResponseEntity<TourListResponseDTO> getAvailableTours(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int pageSize,
            @RequestParam(defaultValue = "Any destination") String destination,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String duration,
            @RequestParam(defaultValue = "1") int adults,
            @RequestParam(defaultValue = "0") int children,
            @RequestParam(required = false) String mealPlan,
            @RequestParam(required = false) String tourType,
            @RequestParam(defaultValue = "RATING_DESC") String sortBy) {
        log.debug("Fetching tours: page={}, destination={}, sortBy={}", page, destination, sortBy);
        return ResponseEntity.ok(tourService.getAvailableTours(
                page, pageSize, destination, startDate, duration,
                adults, children, mealPlan, tourType, sortBy));
    }

    @Operation(summary = "Get tour details by ID")
    @ApiResponse(responseCode = "200", description = "Tour details")
    @ApiResponse(responseCode = "404", description = "Tour not found")
    @GetMapping("/{id}")
    public ResponseEntity<TourDetailsDTO> getTourById(@PathVariable String id) {
        return ResponseEntity.ok(tourService.getTourById(id));
    }

    @Operation(summary = "Get reviews for a specific tour")
    @ApiResponse(responseCode = "200", description = "Paginated reviews")
    @ApiResponse(responseCode = "404", description = "Tour not found")
    @GetMapping("/{id}/reviews")
    public ResponseEntity<ReviewListResponseDTO> getReviews(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "4") int pageSize,
            @RequestParam(defaultValue = "RATING_DESC") String sortBy) {
        return ResponseEntity.ok(tourService.getReviews(id, page, pageSize, sortBy));
    }

    @Operation(summary = "Submit feedback for a tour")
    @ApiResponse(responseCode = "201", description = "Feedback submitted successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input or no valid booking")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Tour not found")
    @ApiResponse(responseCode = "409", description = "Feedback already submitted")
    @PostMapping("/{id}/feedbacks")
    public ResponseEntity<MessageResponseDTO> submitFeedback(
            @PathVariable String id,
            @Valid @RequestBody TourFeedbackRequestDTO request) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tourService.submitFeedback(id, request, userId));
    }

    @Operation(summary = "Get current user's feedback for a tour")
    @ApiResponse(responseCode = "200", description = "User's review found")
    @ApiResponse(responseCode = "404", description = "No review found")
    @GetMapping("/{id}/feedbacks/me")
    public ResponseEntity<ReviewDTO> getMyFeedback(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(tourService.getMyFeedback(id, userId));
    }
}
