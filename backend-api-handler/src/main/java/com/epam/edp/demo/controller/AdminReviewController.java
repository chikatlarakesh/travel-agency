package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.admin.AdminReviewDTO;
import com.epam.edp.demo.dto.admin.AdminReviewListResponseDTO;
import com.epam.edp.demo.dto.admin.UpdateVisibilityRequestDTO;
import com.epam.edp.demo.service.AdminReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    /**
     * GET /api/v1/admin/reviews
     * List all reviews with optional filters: rating, tourType, visibility, flagged.
     */
    @GetMapping
    public ResponseEntity<AdminReviewListResponseDTO> getReviews(
            @RequestParam(defaultValue = "1")    int page,
            @RequestParam(defaultValue = "20")   int pageSize,
            @RequestParam(required = false)      Integer rating,
            @RequestParam(required = false)      String tourType,
            @RequestParam(required = false)      String visibility,
            @RequestParam(required = false)      Boolean flagged) {

        AdminReviewListResponseDTO result = adminReviewService.getReviews(
                page, pageSize, rating, tourType, visibility, flagged);
        return ResponseEntity.ok(result);
    }

    /**
     * PATCH /api/v1/admin/reviews/{id}/visibility
     * Change a review's visibility to PUBLISHED or HIDDEN.
     */
    @PatchMapping("/{id}/visibility")
    public ResponseEntity<AdminReviewDTO> updateVisibility(
            @PathVariable String id,
            @Valid @RequestBody UpdateVisibilityRequestDTO request,
            @AuthenticationPrincipal String adminId) {

        AdminReviewDTO updated = adminReviewService.updateVisibility(id, request.getVisibility(), adminId);
        return ResponseEntity.ok(updated);
    }
}
