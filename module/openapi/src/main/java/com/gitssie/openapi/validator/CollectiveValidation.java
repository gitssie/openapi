package com.gitssie.openapi.validator;

import java.util.Arrays;

/**
 * {@code CollectiveValidation} implements PGV validators for the collective {@code in} and {@code notIn} rules.
 */
public final class CollectiveValidation {
    private CollectiveValidation() {
    }

    public static <T> String in(String field, T value, T[] set)  {
        for (T i : set) {
            if (value.equals(i)) {
                return null;
            }
        }

        return "must be in " + Arrays.toString(set);
    }

    public static <T> String notIn(String field, T value, T[] set) {
        for (T i : set) {
            if (value.equals(i)) {
                return "must not be in " + Arrays.toString(set);
            }
        }
        return null;
    }
}
