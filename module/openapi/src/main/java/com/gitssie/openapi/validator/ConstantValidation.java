package com.gitssie.openapi.validator;

/**
 * {@code ConstantValidation} implements PVG validators for constant values.
 */
public final class ConstantValidation {
    private ConstantValidation() {
    }

    public static <T> String constant(String field, T value, T expected)  {
        if (!value.equals(expected)) {
            return "must equal " + expected.toString();
        }
        return null;
    }
}
