package com.epam.edp.demo.dto.tour;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DestinationListResponseDTO {
    private List<String> destinations;
}
