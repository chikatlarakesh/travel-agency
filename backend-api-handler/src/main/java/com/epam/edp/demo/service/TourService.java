package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.tour.DestinationListResponseDTO;
import com.epam.edp.demo.dto.tour.ReviewDTO;
import com.epam.edp.demo.dto.tour.ReviewListResponseDTO;
import com.epam.edp.demo.dto.tour.TourDetailsDTO;
import com.epam.edp.demo.dto.tour.TourFeedbackRequestDTO;
import com.epam.edp.demo.dto.tour.TourListResponseDTO;
import com.epam.edp.demo.dto.user.MessageResponseDTO;

public interface TourService {

    DestinationListResponseDTO getDestinations(String query);

    TourListResponseDTO getAvailableTours(int page, int pageSize, String destination, String startDate,
                                          String duration, int adults, int children, String mealPlan,
                                          String tourType, String sortBy);

    TourDetailsDTO getTourById(String id);

    ReviewListResponseDTO getReviews(String tourId, int page, int pageSize, String sortBy);

    MessageResponseDTO submitFeedback(String tourId, TourFeedbackRequestDTO request, String userId);

    ReviewDTO getMyFeedback(String tourId, String userId);
}
