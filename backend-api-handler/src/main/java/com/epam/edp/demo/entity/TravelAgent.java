package com.epam.edp.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "travel_agents")
public class TravelAgent {
    @Id
    private String id;
    private String name;
    private String email;
    private String phone;
    private String messenger;
}
