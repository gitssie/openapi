package com.gitssie.openapi.validator;

import java.util.Comparator;

/**
 * {@code ComparativeValidation} implements PGV validation rules for ordering relationships.
 */
public final class ComparativeValidation {
    private ComparativeValidation() {
    }

    public static <T> String lessThan(String field, T value, T limit, Comparator<T> comparator)  {
        if (!lt(comparator.compare(value, limit))) {
            return "must be less than " + limit.toString();
        }
        return null;
    }

    public static <T> String lessThanOrEqual(String field, T value, T limit, Comparator<T> comparator)  {
        if (!lte(comparator.compare(value, limit))) {
            return "must be less than or equal to " + limit.toString();
        }
        return null;
    }

    public static <T> String greaterThan(String field, T value, T limit, Comparator<T> comparator)  {
        if (!gt(comparator.compare(value, limit))) {
            return "must be greater than " + limit.toString();
        }
        return null;
    }

    public static <T> String greaterThanOrEqual(String field, T value, T limit, Comparator<T> comparator)  {
        if (!gte(comparator.compare(value, limit))) {
            return "must be greater than or equal to " + limit.toString();
        }
        return null;
    }

    public static <T> void range(String field, T value, T lt, T lte, T gt, T gte, Comparator<T> comparator)  {
        T ltx = first(lt, lte);
        boolean ltxInc = lte != null;

        T gtx = first(gt, gte);
        boolean gtxInc = gte != null;

        // Inverting the values of lt(e) and gt(e) is valid and creates an exclusive range.
        // {gte:30, lt: 40} => x must be in the range [30, 40)
        // {lt:30, gte:40} => x must be outside the range [30, 40)
        if (lte(comparator.compare(gtx, ltx))) {
            between(field, value, gtx, gtxInc, ltx, ltxInc, comparator);
        } else {
            outside(field, value, ltx, !ltxInc, gtx, !gtxInc, comparator);
        }
    }

    public static <T> String between(String field, T value, T lower, boolean lowerInclusive, T upper, boolean upperInclusive, Comparator<T> comparator)  {
        if (!between(value, lower, lowerInclusive, upper, upperInclusive, comparator)) {
            return "must be in the range " + range(lower, lowerInclusive, upper, upperInclusive);
        }
        return null;
    }

    public static <T> String outside(String field, T value, T lower, boolean lowerInclusive, T upper, boolean upperInclusive, Comparator<T> comparator)  {
        if (between(value, lower, lowerInclusive, upper, upperInclusive, comparator)) {
            return "must be outside the range " + range(lower, lowerInclusive, upper, upperInclusive);
        }
        return null;
    }

    private static <T> boolean between(T value, T lower, boolean lowerInclusive, T upper, boolean upperInclusive, Comparator<T> comparator) {
        return (lowerInclusive ? gte(comparator.compare(value, lower)) : gt(comparator.compare(value, lower))) &&
               (upperInclusive ? lte(comparator.compare(value, upper)) : lt(comparator.compare(value, upper)));
    }

    private static <T> String range(T lower, boolean lowerInclusive, T upper, boolean upperInclusive) {
        return (lowerInclusive ? "[" : "(") + lower.toString() + ", " + upper.toString() + (upperInclusive ? "]" : ")");
    }

    private static boolean lt(int comparatorResult) {
        return comparatorResult < 0;
    }

    private static boolean lte(int comparatorResult) {
        return comparatorResult <= 0;
    }

    private static boolean gt(int comparatorResult) {
        return comparatorResult > 0;
    }

    private static boolean gte(int comparatorResult) {
        return comparatorResult >= 0;
    }

    private static <T> T first(T lhs, T rhs) {
        return lhs != null ? lhs : rhs;
    }
}
