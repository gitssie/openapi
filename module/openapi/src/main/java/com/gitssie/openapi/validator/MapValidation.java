package com.gitssie.openapi.validator;

import java.util.Collection;
import java.util.Map;

/**
 * {@code MapValidation} implements PGV validation for protobuf {@code Map} fields.
 */
public final class MapValidation {
    private MapValidation() {
    }

    public static String min(String field, Map value, int expected)  {
        if (Math.min(value.size(), expected) != expected) {
            return "value size must be at least " + expected;
        }
        return null;
    }

    public static String max(String field, Map value, int expected)  {
        if (Math.max(value.size(), expected) != expected) {
            return "value size must not exceed " + expected;
        }
        return null;
    }

    public static void noSparse(String field, Map value)  {
        //throw new UnimplementedException(field, "no_sparse validation is not implemented for Java because protobuf maps cannot be sparse in Java");
    }

    @FunctionalInterface
    public interface MapValidator<T> {
        void accept(T val) ;
    }

    public static <T> void validateParts(Collection<T> vals, MapValidator<T> validator)  {
       for (T val : vals) {
           validator.accept(val);
       }
    }
}
