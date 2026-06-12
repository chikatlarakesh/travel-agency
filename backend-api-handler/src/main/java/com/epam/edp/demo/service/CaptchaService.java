package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.auth.CaptchaResponseDTO;

/**
 * Contract for custom CAPTCHA generation and validation.
 *
 * <p>Implementations must guarantee:
 * <ul>
 *   <li>Each generated CAPTCHA has a unique, opaque ID.</li>
 *   <li>A CAPTCHA can only be validated once (single-use).</li>
 *   <li>CAPTCHAs expire after a configurable TTL.</li>
 * </ul>
 */
public interface CaptchaService {

    /**
     * Generates a new CAPTCHA challenge.
     *
     * @return a {@link CaptchaResponseDTO} containing the captchaId and
     *         a Base64-encoded PNG image.
     */
    CaptchaResponseDTO generate();

    /**
     * Validates the user's CAPTCHA answer against the stored challenge.
     *
     * <p>The entry is invalidated immediately after this call, regardless
     * of outcome, to prevent reuse and timing attacks.
     *
     * @param captchaId     the ID returned when the CAPTCHA was generated.
     * @param captchaAnswer the text entered by the user.
     * @throws com.epam.edp.demo.exception.CaptchaValidationException on
     *         missing, expired, or incorrect CAPTCHA.
     */
    void validate(String captchaId, String captchaAnswer);
}
