package com.gitssie.openapi.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.vavr.control.Either;
import org.apache.commons.lang3.time.FastDateFormat;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Libs {
    private final static Pattern PATTERN_INDEX = Pattern.compile("^\\[(\\d+)\\]$");
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static int seconds() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public static <T> Either<String, T> mustRight(Either<String, T> either) {
        if (either.isLeft()) {
            throw new IllegalStateException(either.getLeft());
        }
        return either;
    }

    public static String formatDate(Date date) {
        return FastDateFormat.getInstance(DATE_FORMAT).format(date);
    }

    public static String formatDateTime(Date date) {
        return FastDateFormat.getInstance(DATETIME_FORMAT).format(date);
    }

    public static Date parseDate(String date) {
        try {
            return FastDateFormat.getInstance(DATE_FORMAT).parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static Date parseDateTime(String date) {
        try {
            return FastDateFormat.getInstance(DATETIME_FORMAT).parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Object> toMap(String... pairs) {
        Map<String, Object> map = Maps.newHashMap();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], i + 1 < pairs.length ? pairs[i + 1] : null);
        }
        return map;
    }

    public static List<Map<String, Object>> toMapList(String... pairs) {
        return Lists.newArrayList(toMap(pairs));
    }


    /**
     * Extracts a value from a target object using DPath expression.
     *
     * @param target
     * @param dPath
     */
    public static Object pathGet(Object target, String dPath) {
        String[] paths = dPath.split("\\.");
        Object result = target;
        for (String path : paths) {
            result = extractValue(result, path);
        }
        return result;
    }

    private static Object extractValue(Object target, String index) {
        if (target == null) {
            return null;
        }
        Matcher m = PATTERN_INDEX.matcher(index);
        if (m.matches()) {
            int i = Integer.parseInt(m.group(1));
            if (target instanceof Object[]) {
                return ((Object[]) target)[i];
            }
            if (target instanceof List<?>) {
                return ((List<?>) target).get(i);
            }
            throw new IllegalArgumentException("Expect an array or list!");
        }
        if (target instanceof Map<?, ?>) {
            return ((Map<?, ?>) target).get(index);
        }
        throw new IllegalArgumentException();
    }
}
