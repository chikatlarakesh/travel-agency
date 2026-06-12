package com.epam.edp.demo.dto.tour;

import lombok.Data;

@Data
public class ReviewDTO {
    private String authorName;
    private String authorImageUrl;
    private String createdAt;
    private int rate;
    private String reviewContent;
}
