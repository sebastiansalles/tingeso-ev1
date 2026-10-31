package cl.ipt.siga.entity.enums;

import cl.ipt.siga.exception.BusinessRuleException;

public enum Day {
    MONDAY('L'),
    TUESDAY('M'),
    WEDNESDAY('W'),
    THURSDAY('J'),
    FRIDAY('V');

    private final char code;   // letter used by the institute's schedule codes (L2, W3...)

    Day(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static Day fromCode(char code) {
        for (Day day : values()) {
            if (day.code == Character.toUpperCase(code)) {
                return day;
            }
        }
        throw new BusinessRuleException("Invalid day code: " + code + " (valid: L, M, W, J, V)");
    }
}