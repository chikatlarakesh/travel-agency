package com.epam.edp.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "reviews")
@CompoundIndex(
        name = "idx_user_tour_unique",
        def = "{'userId': 1, 'tourId': 1}",
        unique = true,
        sparse = true
)
public class Review {
    @Id
    private String id;
    @Indexed
    private String tourId;
    @Indexed
    private String userId;
    private String authorName;
    private String authorImageUrl;
    private String createdAt;
    private int rate;
    private String reviewContent;

    // Denormalized tour metadata — stored on submission for efficient admin queries
    private String tourName;
    private String tourType;

    // Moderation fields
    @Indexed
    private String visibility = "PUBLISHED"; // "PUBLISHED" or "HIDDEN"
    private boolean flagged = false;
    private String flagReason;
}
