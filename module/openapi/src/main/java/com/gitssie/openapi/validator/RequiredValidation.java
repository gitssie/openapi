package com.gitssie.openapi.validator;

/**
 * {@code RequiredValidation} implements PGV validation for required fields.
 */
public final class RequiredValidation {
    private RequiredValidation() {
    }

    public static String required(String field, Object value)  {
        if (value == null) {
            return field + "is required";
        }
        return null;
    }
}
