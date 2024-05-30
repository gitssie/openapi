package com.gitssie.openapi.validator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@code RepeatedValidation} implements PGV validators for collection-type validators.
 */
public final class RepeatedValidation {
    private RepeatedValidation() {
    }

    public static <T> String minItems(String field, List<T> values, int expected)  {
        if (values.size() < expected) {
            return "must have at least " + expected + " items";
        }
        return null;
    }

    public static <T> String maxItems(String field, List<T> values, int expected)  {
        if (values.size() > expected) {
            return "must have at most " + expected + " items";
        }
        return null;
    }

    public static <T> String unique(String field, List<T> values)  {
        Set<T> seen = new HashSet<>();
        for (T value : values) {
            // Abort at the first sign of a duplicate
            if (!seen.add(value)) {
                return "must have all unique values";
            }
        }
        return null;
    }

    @FunctionalInterface
    public interface ValidationConsumer<T> {
        void accept(T value) ;
    }

    public static <T> void forEach(List<T> values, ValidationConsumer<T> consumer)  {
        for (T value : values) {
            consumer.accept(value);
        }
    }
}
