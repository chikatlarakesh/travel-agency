package com.epam.edp.demo.dto.tour;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GuestQuantityDTO {

    private int adultsMaxValue;

    private int childrenMaxValue;

    /** Field name mirrors the spec's intentional typo ("Velue" not "Value"). */
    private int totalMaxVelue;
}
