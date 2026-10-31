package cl.ipt.siga.util;

import cl.ipt.siga.exception.BusinessRuleException;

// Text normalization and validation shared by students and teachers
public final class PersonDataUtils {

    private PersonDataUtils() { }   // utility class: it is never instantiated

    // "12.345.678-k", "12345678-K" and "12345678k" all become "12345678-K"
    public static String normalizeRun(String run) {
        if (run == null) {
            throw new BusinessRuleException("RUN is required");
        }
        String clean = run.replace(".", "").replace("-", "").replace(" ", "").toUpperCase();
        if (!clean.matches("\\d{7,8}[0-9K]")) {
            throw new BusinessRuleException("Invalid RUN format: " + run);
        }
        return clean.substring(0, clean.length() - 1) + "-" + clean.charAt(clean.length() - 1);
    }

    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException("Email is required");
        }
        return email.trim().toLowerCase();
    }

    // Required text fields: avoids a 500 from the database when a field is missing
    public static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(fieldName + " is required");
        }
        return value.trim();
    }
}