package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.auth.CaptchaResponseDTO;
import com.epam.edp.demo.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and session management")
public class CaptchaController {

    private final CaptchaService captchaService;

    @Operation(
        summary = "Generate a CAPTCHA challenge",
        description = "Returns a unique captchaId and a Base64-encoded PNG image. " +
                      "The client must display the image, collect the user's answer, and " +
                      "submit both captchaId and captchaAnswer in the sign-up request body."
    )
    @ApiResponse(responseCode = "200", description = "CAPTCHA generated successfully")
    @GetMapping("/api/v1/auth/captcha/generate")
    public ResponseEntity<CaptchaResponseDTO> generateCaptcha() {
        CaptchaResponseDTO response = captchaService.generate();
        log.debug("captcha.endpoint.generate captchaId={}", response.getCaptchaId());
        return ResponseEntity.ok(response);
    }
}
