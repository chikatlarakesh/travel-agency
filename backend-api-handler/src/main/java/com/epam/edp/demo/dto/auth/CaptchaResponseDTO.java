package com.epam.edp.demo.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response payload returned by {@code GET /api/v1/auth/captcha/generate}.
 *
 * <p>The frontend should:
 * <ol>
 *   <li>Display the {@code imageBase64} as an {@code <img>} tag (data URL included).</li>
 *   <li>Store {@code captchaId} and send it with the sign-up request.</li>
 * </ol>
 */
@Data
@AllArgsConstructor
public class CaptchaResponseDTO {

    /** Opaque UUID that identifies this CAPTCHA challenge on the server. */
    private String captchaId;

    /**
     * A {@code data:image/png;base64,...} encoded string that can be set
     * directly as the {@code src} attribute of an HTML img element.
     */
    private String imageBase64;

    /** Seconds remaining before the CAPTCHA token expires. */
    private int expiresInSeconds;
}
