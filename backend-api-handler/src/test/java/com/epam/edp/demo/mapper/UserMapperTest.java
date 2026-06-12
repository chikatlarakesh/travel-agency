package com.epam.edp.demo.mapper;

import com.epam.edp.demo.dto.auth.SignInResponseDTO;
import com.epam.edp.demo.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void toSignInResponse_mapsAllFieldsCorrectly() {
        User user = User.builder()
                .id("u-1").firstName("Jane").lastName("Smith")
                .email("jane@example.com").passwordHash("hashed").role("TRAVEL_AGENT")
                .build();

        SignInResponseDTO dto = mapper.toSignInResponse(user, "my-token");

        assertThat(dto.getIdToken()).isEqualTo("my-token");
        assertThat(dto.getRole()).isEqualTo("TRAVEL_AGENT");
        assertThat(dto.getUserName()).isEqualTo("Jane Smith");
        assertThat(dto.getEmail()).isEqualTo("jane@example.com");
    }
}
