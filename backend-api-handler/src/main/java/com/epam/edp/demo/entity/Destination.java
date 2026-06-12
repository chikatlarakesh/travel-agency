package com.epam.edp.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;

/**
 * Embedded destination value object stored inside the Tour document.
 *
 * Both fields are individually indexed so that search queries filtering
 * on destination.city or destination.country hit an index directly.
 * Spring Data MongoDB resolves the full dotted path from the parent @Document.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Destination {

    @Indexed
    private String city;

    @Indexed
    private String country;
}
