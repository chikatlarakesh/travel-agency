package com.epam.edp.demo.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReviewDTO {
    private String id;
    private String tourId;
    private String tourName;
    private String tourType;
    private String userId;
    private String authorName;
    private String authorImageUrl;
    private String createdAt;
    private int rate;
    private String reviewContent;
    private String visibility;
    private boolean flagged;
    private String flagReason;
}
