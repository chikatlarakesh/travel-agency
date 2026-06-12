package com.epam.edp.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embedded hotel value object stored inside the Tour document.
 * Intentionally not a @Document — it lives inside the tours collection.
 *
 * starRating uses int (1–5); kept as a primitive to avoid null ambiguity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hotel {

    private String name;

    private String description;

    /** Star rating 1–5. */
    private int starRating;
}
