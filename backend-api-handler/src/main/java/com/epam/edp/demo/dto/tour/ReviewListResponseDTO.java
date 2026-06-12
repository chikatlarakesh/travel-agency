package com.epam.edp.demo.dto.tour;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewListResponseDTO {
    private List<ReviewDTO> reviews;
    private int page;
    private int pageSize;
    private int totalPages;
    private int totalItems;
}
