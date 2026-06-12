package com.epam.edp.demo.dto.tour;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated wrapper returned by the tour search / list endpoint.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TourListResponseDTO {

    private List<TourSummaryDTO> tours;

    private int page;

    private int pageSize;

    private int totalPages;

    private int totalItems;
}
