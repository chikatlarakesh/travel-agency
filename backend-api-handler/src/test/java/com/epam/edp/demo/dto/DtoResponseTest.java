package com.epam.edp.demo.dto;

import com.epam.edp.demo.dto.response.ApiResponse;
import com.epam.edp.demo.dto.response.AuthResponse;
import com.epam.edp.demo.dto.response.ErrorResponse;
import com.epam.edp.demo.dto.response.MessageResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class DtoResponseTest {

    // ── ApiResponse ───────────────────────────────────────────────────────────

    @Test
    public void apiResponse_successWithData() {
        ApiResponse<String> resp = ApiResponse.success("hello");
        assertTrue(resp.isSuccess());
        assertEquals("hello", resp.getData());
        assertNull(resp.getError());
    }

    @Test
    public void apiResponse_successWithMessageAndData() {
        ApiResponse<Integer> resp = ApiResponse.success("done", 42);
        assertTrue(resp.isSuccess());
        assertEquals("done", resp.getMessage());
        assertEquals(42, (int) resp.getData());
    }

    @Test
    public void apiResponse_errorWithErrorString() {
        ApiResponse<Void> resp = ApiResponse.error("something failed");
        assertFalse(resp.isSuccess());
        assertEquals("something failed", resp.getError());
        assertNull(resp.getData());
    }

    @Test
    public void apiResponse_errorWithMessageAndError() {
        ApiResponse<Void> resp = ApiResponse.error("context", "detail");
        assertFalse(resp.isSuccess());
        assertEquals("context", resp.getMessage());
        assertEquals("detail", resp.getError());
    }

    @Test
    public void apiResponse_builderDirectly() {
        ApiResponse<String> resp = ApiResponse.<String>builder()
                .success(true)
                .message("msg")
                .data("value")
                .error("err")
                .build();
        assertTrue(resp.isSuccess());
        assertEquals("msg", resp.getMessage());
        assertEquals("value", resp.getData());
        assertEquals("err", resp.getError());
    }

    // ── AuthResponse ──────────────────────────────────────────────────────────

    @Test
    public void authResponse_builderAndGetters() {
        AuthResponse resp = AuthResponse.builder()
                .accessToken("access-token")
                .tokenType("Bearer")
                .expiresIn(900)
                .refreshTokenRaw("refresh-raw")
                .role("CUSTOMER")
                .userName("Alice")
                .email("alice@example.com")
                .build();
        assertEquals("access-token", resp.getAccessToken());
        assertEquals("Bearer", resp.getTokenType());
        assertEquals(900, resp.getExpiresIn());
        assertEquals("refresh-raw", resp.getRefreshTokenRaw());
        assertEquals("CUSTOMER", resp.getRole());
        assertEquals("Alice", resp.getUserName());
        assertEquals("alice@example.com", resp.getEmail());
    }

    @Test
    public void authResponse_defaultTokenType_isBearer() {
        AuthResponse resp = AuthResponse.builder().accessToken("tok").build();
        assertEquals("Bearer", resp.getTokenType());
    }

    // ── ErrorResponse ───────────────────────────────────────────────────

    @Test
    public void errorResponse_staticFactoryWithErrorOnly() {
        ErrorResponse err = ErrorResponse.of("Not Found");
        assertNotNull(err);
        assertEquals("Not Found", err.getError());
        assertNull(err.getDetails());
    }

    @Test
    public void errorResponse_staticFactoryWithDetails() {
        java.util.List<String> details = java.util.List.of("field required");
        ErrorResponse err = ErrorResponse.of("Validation failed", details);
        assertEquals("Validation failed", err.getError());
        assertEquals(1, err.getDetails().size());
    }

    // ── MessageResponse ───────────────────────────────────────────────────────

    @Test
    public void messageResponse_setsMessage() {
        MessageResponse msg = new MessageResponse("Operation successful");
        assertEquals("Operation successful", msg.getMessage());
    }
    @Test
    public void messageResponse_staticFactory() {
        MessageResponse msg = MessageResponse.of("done");
        assertEquals("done", msg.getMessage());
    }}
