package com.epam.edp.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Scans review text for prohibited keywords and auto-flags content for manual admin review.
 * Keyword list is intentionally conservative — false positives are caught by admin moderation.
 */
@Slf4j
@Service
public class ContentModerationService {

    private static final Set<String> PROHIBITED_KEYWORDS = Set.of(
        "spam", "scam", "fraud", "fake", "cheat", "steal", "idiot", "stupid",
        "moron", "hate", "kill", "threat", "illegal", "racist", "sexist",
        "offensive", "obscene", "porn", "xxx", "drug", "casino", "bitcoin",
        "click here", "free money", "winner", "prize", "buy now", "discount code"
    );

    /**
     * Returns true if the text contains any prohibited keyword (case-insensitive).
     */
    public boolean containsProhibitedContent(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase();
        return PROHIBITED_KEYWORDS.stream().anyMatch(lower::contains);
    }

    /**
     * Returns a short reason string listing which keywords triggered the flag.
     */
    public String getFlagReason(String text) {
        if (text == null || text.isBlank()) return null;
        String lower = text.toLowerCase();
        String matched = PROHIBITED_KEYWORDS.stream()
                .filter(lower::contains)
                .reduce((a, b) -> a + ", " + b)
                .orElse(null);
        return matched != null ? "Prohibited keywords detected: " + matched : null;
    }
}
