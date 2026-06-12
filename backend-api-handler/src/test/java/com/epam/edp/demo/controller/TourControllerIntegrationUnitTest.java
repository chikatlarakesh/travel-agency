package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.tour.DestinationListResponseDTO;
import com.epam.edp.demo.dto.tour.ReviewDTO;
import com.epam.edp.demo.dto.tour.ReviewListResponseDTO;
import com.epam.edp.demo.dto.tour.TourDetailsDTO;
import com.epam.edp.demo.dto.tour.TourListResponseDTO;
import com.epam.edp.demo.exception.TourNotFoundException;
import com.epam.edp.demo.security.JwtAuthenticationFilter;
import com.epam.edp.demo.security.JwtTokenProvider;
import com.epam.edp.demo.security.RateLimitFilter;
import com.epam.edp.demo.service.TourService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-layer unit tests for GET /api/v1/tours/{id} and GET /api/v1/tours/{id}/reviews.
 *
 * Uses @WebMvcTest to load only the web layer (TourController + GlobalExceptionHandler).
 * TourService is mocked — no MongoDB connection required.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(value = TourController.class, excludeAutoConfiguration = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
public class TourControllerIntegrationUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TourService tourService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitFilter rateLimitFilter;

    // ── GET /api/v1/tours/{id} ────────────────────────────────────────────

    @Test
    public void getTourById_validId_returns200WithTourDetails() throws Exception {
        TourDetailsDTO dto = new TourDetailsDTO();
        dto.setId("t-001");
        dto.setName("Garden Resort");
        dto.setDestination("Punta Cana, Dominican Republic");
        dto.setRating(4.8);
        when(tourService.getTourById("t-001")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/tours/t-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("t-001"))
                .andExpect(jsonPath("$.name").value("Garden Resort"))
                .andExpect(jsonPath("$.destination").value("Punta Cana, Dominican Republic"))
                .andExpect(jsonPath("$.rating").value(4.8));
    }

    @Test
    public void getTourById_unknownId_returns404WithErrorMessage() throws Exception {
        when(tourService.getTourById("unknown-id"))
                .thenThrow(new TourNotFoundException("unknown-id"));

        mockMvc.perform(get("/api/v1/tours/unknown-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Tour not found: unknown-id"));
    }

    @Test
    public void getTourById_tourWithNullRating_returns200WithoutError() throws Exception {
        TourDetailsDTO dto = new TourDetailsDTO();
        dto.setId("t-002");
        dto.setName("Unrated Tour");
        when(tourService.getTourById("t-002")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/tours/t-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("t-002"))
                .andExpect(jsonPath("$.name").value("Unrated Tour"));
    }

    @Test
    public void getTourById_tourWithEmptyImageUrls_returns200WithEmptyArray() throws Exception {
        TourDetailsDTO dto = new TourDetailsDTO();
        dto.setId("t-003");
        dto.setImageUrls(Collections.emptyList());
        when(tourService.getTourById("t-003")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/tours/t-003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrls").isArray())
                .andExpect(jsonPath("$.imageUrls").isEmpty());
    }

    @Test
    public void getTourById_tourWithAllFields_allFieldsSerializedCorrectly() throws Exception {
        TourDetailsDTO dto = new TourDetailsDTO();
        dto.setId("t-004");
        dto.setName("Dolomites Hike");
        dto.setDestination("Dolomites, Italy");
        dto.setRating(4.7);
        dto.setSummary("An epic mountain hike");
        dto.setHotelName("Mountain Lodge");
        dto.setFreeCancelationDaysBefore(7);
        dto.setImageUrls(Arrays.asList("https://example.com/img1.jpg", "https://example.com/img2.jpg"));
        dto.setDurations(Arrays.asList("7 days", "10 days"));
        dto.setMealPlans(Arrays.asList("BB", "HB"));
        when(tourService.getTourById("t-004")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/tours/t-004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("t-004"))
                .andExpect(jsonPath("$.summary").value("An epic mountain hike"))
                .andExpect(jsonPath("$.hotelName").value("Mountain Lodge"))
                .andExpect(jsonPath("$.freeCancelationDaysBefore").value(7))
                .andExpect(jsonPath("$.imageUrls.length()").value(2))
                .andExpect(jsonPath("$.durations.length()").value(2))
                .andExpect(jsonPath("$.mealPlans.length()").value(2));
    }

    @Test
    public void getTourById_serviceThrowsRuntimeException_returns500WithGenericMessage() throws Exception {
        when(tourService.getTourById("t-err"))
                .thenThrow(new RuntimeException("DB connection failed"));

        mockMvc.perform(get("/api/v1/tours/t-err"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"));
    }

    // ── GET /api/v1/tours/{id}/reviews ────────────────────────────────────

    @Test
    public void getReviews_validTourWithReviews_returns200WithPaginatedBody() throws Exception {
        ReviewDTO r1 = buildReview("Alice", "https://img.com/a.jpg", "2024-08-01", 5, "Excellent!");
        ReviewDTO r2 = buildReview("Bob", "https://img.com/b.jpg", "2024-07-01", 4, "Very good");
        ReviewListResponseDTO resp = new ReviewListResponseDTO(Arrays.asList(r1, r2), 1, 4, 1, 2);
        when(tourService.getReviews("t-001", 1, 4, "RATING_DESC")).thenReturn(resp);

        mockMvc.perform(get("/api/v1/tours/t-001/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews").isArray())
                .andExpect(jsonPath("$.reviews.length()").value(2))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(4));
    }

    @Test
    public void getReviews_unknownTourId_returns404WithErrorMessage() throws Exception {
        when(tourService.getReviews(eq("bad-id"), anyInt(), anyInt(), anyString()))
                .thenThrow(new TourNotFoundException("bad-id"));

        mockMvc.perform(get("/api/v1/tours/bad-id/reviews"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Tour not found: bad-id"));
    }

    @Test
    public void getReviews_tourWithNoReviews_returns200WithEmptyList() throws Exception {
        ReviewListResponseDTO resp = new ReviewListResponseDTO(
                Collections.emptyList(), 1, 4, 0, 0);
        when(tourService.getReviews(eq("t-empty"), anyInt(), anyInt(), anyString()))
                .thenReturn(resp);

        mockMvc.perform(get("/api/v1/tours/t-empty/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews").isEmpty())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    public void getReviews_customPaginationParams_forwardedToService() throws Exception {
        ReviewListResponseDTO resp = new ReviewListResponseDTO(
                Collections.emptyList(), 2, 2, 3, 5);
        when(tourService.getReviews("t-001", 2, 2, "RATING_DESC")).thenReturn(resp);

        mockMvc.perform(get("/api/v1/tours/t-001/reviews")
                        .param("page", "2")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.pageSize").value(2));
    }

    @Test
    public void getReviews_sortByNewest_paramForwardedToService() throws Exception {
        ReviewListResponseDTO resp = new ReviewListResponseDTO(Collections.emptyList(), 1, 4, 0, 0);
        when(tourService.getReviews("t-001", 1, 4, "NEWEST")).thenReturn(resp);

        mockMvc.perform(get("/api/v1/tours/t-001/reviews").param("sortBy", "NEWEST"))
                .andExpect(status().isOk());
    }

    @Test
    public void getReviews_sortByOldest_paramForwardedToService() throws Exception {
        ReviewListResponseDTO resp = new ReviewListResponseDTO(Collections.emptyList(), 1, 4, 0, 0);
        when(tourService.getReviews("t-001", 1, 4, "OLDEST")).thenReturn(resp);

        mockMvc.perform(get("/api/v1/tours/t-001/reviews").param("sortBy", "OLDEST"))
                .andExpect(status().isOk());
    }

    @Test
    public void getReviews_sortByRatingAsc_paramForwardedToService() throws Exception {
        ReviewListResponseDTO resp = new ReviewListResponseDTO(Collections.emptyList(), 1, 4, 0, 0);
        when(tourService.getReviews("t-001", 1, 4, "RATING_ASC")).thenReturn(resp);

        mockMvc.perform(get("/api/v1/tours/t-001/reviews").param("sortBy", "RATING_ASC"))
                .andExpect(status().isOk());
    }

    @Test
    public void getReviews_reviewFieldsMappedCorrectly_allFieldsInResponse() throws Exception {
        ReviewDTO r = buildReview("Carol", "https://example.com/carol.jpg", "2024-12-01", 5, "Amazing!");
        ReviewListResponseDTO resp = new ReviewListResponseDTO(
                Collections.singletonList(r), 1, 4, 1, 1);
        when(tourService.getReviews(eq("t-001"), anyInt(), anyInt(), anyString())).thenReturn(resp);

        mockMvc.perform(get("/api/v1/tours/t-001/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews[0].authorName").value("Carol"))
                .andExpect(jsonPath("$.reviews[0].authorImageUrl").value("https://example.com/carol.jpg"))
                .andExpect(jsonPath("$.reviews[0].createdAt").value("2024-12-01"))
                .andExpect(jsonPath("$.reviews[0].rate").value(5))
                .andExpect(jsonPath("$.reviews[0].reviewContent").value("Amazing!"));
    }

    @Test
    public void getReviews_reviewWithNullAuthorImage_returns200WithoutError() throws Exception {
        ReviewDTO r = buildReview("Dave", null, "2024-01-15", 4, "Good trip");
        ReviewListResponseDTO resp = new ReviewListResponseDTO(
                Collections.singletonList(r), 1, 4, 1, 1);
        when(tourService.getReviews(eq("t-001"), anyInt(), anyInt(), anyString())).thenReturn(resp);

        mockMvc.perform(get("/api/v1/tours/t-001/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews[0].authorName").value("Dave"))
                .andExpect(jsonPath("$.reviews[0].rate").value(4));
    }

    @Test
    public void getReviews_serviceThrowsRuntimeException_returns500WithGenericMessage() throws Exception {
        when(tourService.getReviews(eq("t-err"), anyInt(), anyInt(), anyString()))
                .thenThrow(new RuntimeException("DB connection failed"));

        mockMvc.perform(get("/api/v1/tours/t-err/reviews"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"));
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private ReviewDTO buildReview(String author, String imageUrl, String createdAt,
                                   int rate, String content) {
        ReviewDTO dto = new ReviewDTO();
        dto.setAuthorName(author);
        dto.setAuthorImageUrl(imageUrl);
        dto.setCreatedAt(createdAt);
        dto.setRate(rate);
        dto.setReviewContent(content);
        return dto;
    }

    // ── GET /api/v1/tours/destinations ────────────────────────────────────

    @Test
    public void getDestinations_withQuery_returns200WithList() throws Exception {
        DestinationListResponseDTO response =
                new DestinationListResponseDTO(Arrays.asList("Rome, Italy", "Rotterdam, Netherlands"));
        when(tourService.getDestinations("rom")).thenReturn(response);

        mockMvc.perform(get("/api/v1/tours/destinations").param("destination", "rom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinations").isArray())
                .andExpect(jsonPath("$.destinations.length()").value(2))
                .andExpect(jsonPath("$.destinations[0]").value("Rome, Italy"));
    }

    @Test
    public void getDestinations_noMatches_returns200WithEmptyList() throws Exception {
        when(tourService.getDestinations("xyz")).thenReturn(new DestinationListResponseDTO(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/tours/destinations").param("destination", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinations").isEmpty());
    }

    // ── GET /api/v1/tours/available ───────────────────────────────────────

    @Test
    public void getAvailableTours_defaultParams_returns200() throws Exception {
        TourListResponseDTO response = new TourListResponseDTO(Collections.emptyList(), 1, 6, 1, 0);
        when(tourService.getAvailableTours(1, 6, "Any destination", null, null, 1, 0, null, null, "RATING_DESC"))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/tours/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tours").isArray())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(6))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    public void getAvailableTours_customParams_forwardsToService() throws Exception {
        TourListResponseDTO response = new TourListResponseDTO(Collections.emptyList(), 2, 3, 4, 10);
        when(tourService.getAvailableTours(2, 3, "Rome, Italy", "2027-06-01", "7 days", 2, 1, "BB", "RESORT", "PRICE_ASC"))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/tours/available")
                        .param("page", "2")
                        .param("pageSize", "3")
                        .param("destination", "Rome, Italy")
                        .param("startDate", "2027-06-01")
                        .param("duration", "7 days")
                        .param("adults", "2")
                        .param("children", "1")
                        .param("mealPlan", "BB")
                        .param("tourType", "RESORT")
                        .param("sortBy", "PRICE_ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.pageSize").value(3))
                .andExpect(jsonPath("$.totalItems").value(10));
    }
}
