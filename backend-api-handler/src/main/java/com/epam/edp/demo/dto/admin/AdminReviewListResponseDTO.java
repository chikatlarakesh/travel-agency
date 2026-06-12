package com.epam.edp.demo.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AdminReviewListResponseDTO {
    private List<AdminReviewDTO> reviews;
    private int page;
    private int pageSize;
    private int totalPages;
    private int totalItems;
}
