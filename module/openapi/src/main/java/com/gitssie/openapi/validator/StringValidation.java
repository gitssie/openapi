package com.gitssie.openapi.validator;

import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * {@code StringValidation} implements PGV validation for protobuf {@code String} fields.
 */
@SuppressWarnings("WeakerAccess")
public final class StringValidation {
    private static final int UUID_DASH_1 = 8;
    private static final int UUID_DASH_2 = 13;
    private static final int UUID_DASH_3 = 18;
    private static final int UUID_DASH_4 = 23;
    private static final int UUID_LEN = 36;

    private StringValidation() {
        // Intentionally left blank.
    }

    // Defers initialization until needed and from there on we keep an object
    // reference and aString future calls; it is safe to assume that we require
    // the instance again after initialization.
    private static class Lazy {
        static final EmailValidator EMAIL_VALIDATOR = EmailValidator.getInstance(true, true);
    }

    public static String length(final String field, final String value, final int expected)  {
        final int actual = value.codePointCount(0, value.length());
        if (actual != expected) {
            return "length must be " + expected + " but got: " + actual;
        }
        return null;
    }

    public static String minLength(final String field, final String value, final int expected)  {
        final int actual = value.codePointCount(0, value.length());
        if (actual < expected) {
            return "length must be " + expected + " but got: " + actual;
        }
        return null;
    }

    public static String maxLength(final String field, final String value, final int expected)  {
        final int actual = value.codePointCount(0, value.length());
        if (actual > expected) {
            return "length must be " + expected + " but got: " + actual;
        }
        return null;
    }

    public static String lenBytes(String field, String value, int expected)  {
        if (value.getBytes(StandardCharsets.UTF_8).length != expected) {
            return "bytes length must be " + expected;
        }
        return null;
    }

    public static String minBytes(String field, String value, int expected)  {
        if (value.getBytes(StandardCharsets.UTF_8).length < expected) {
            return "bytes length must be at least " + expected;
        }
        return null;
    }

    public static String maxBytes(String field, String value, int expected)  {
        if (value.getBytes(StandardCharsets.UTF_8).length > expected) {
            return "bytes length must be at maximum " + expected;
        }
        return null;
    }

    public static String pattern(String field, String value, Pattern p)  {
        if (!p.matcher(value).matches()) {
            return "must match pattern " + p.pattern();
        }
        return null;
    }

    public static String prefix(String field, String value, String prefix)  {
        if (!value.startsWith(prefix)) {
            return "should start with " + prefix;
        }
        return null;
    }

    public static String contains(String field, String value, String contains)  {
        if (!value.contains(contains)) {
            return "should contain " + contains;
        }
        return null;
    }

    public static String notContains(String field, String value, String contains)  {
        if (value.contains(contains)) {
            return "should not contain " + contains;
        }
        return null;
    }


    public static String suffix(String field, String value, String suffix)  {
        if (!value.endsWith(suffix)) {
            return "should end with " + suffix;
        }
        return null;
    }

    public static String email(final String field, String value)  {
        if (value.charAt(value.length() - 1) == '>') {
            final char[] chars = value.toCharArray();
            final StringBuilder sb = new StringBuilder();
            boolean insideQuotes = false;
            for (int i = chars.length - 2; i >= 0; i--) {
                final char c = chars[i];
                if (c == '<') {
                    if (!insideQuotes) break;
                } else if (c == '"') {
                    insideQuotes = !insideQuotes;
                }
                sb.append(c);
            }
            value = sb.reverse().toString();
        }

        if (!Lazy.EMAIL_VALIDATOR.isValid(value)) {
            return "should be a valid email";
        }
        return null;
    }

    public static String address(String field, String value)  {
        boolean validHost = isAscii(value) && DomainValidator.getInstance(true).isValid(value);
        boolean validIp = InetAddressValidator.getInstance().isValid(value);

        if (!validHost && !validIp) {
            return "should be a valid host, or an ip address.";
        }
        return null;
    }

    public static String hostName(String field, String value)  {
        if (!isAscii(value)) {
            return "should be a valid host containing only ascii characters";
        }

        DomainValidator domainValidator = DomainValidator.getInstance(true);
        if (!domainValidator.isValid(value)) {
            return "should be a valid host";
        }
        return null;
    }

    public static String ip(String field, String value)  {
        InetAddressValidator ipValidator = InetAddressValidator.getInstance();
        if (!ipValidator.isValid(value)) {
            return "should be a valid ip address";
        }
        return null;
    }

    public static String ipv4(String field, String value)  {
        InetAddressValidator ipValidator = InetAddressValidator.getInstance();
        if (!ipValidator.isValidInet4Address(value)) {
            return "should be a valid ipv4 address";
        }
        return null;
    }

    public static String ipv6(String field, String value)  {
        InetAddressValidator ipValidator = InetAddressValidator.getInstance();
        if (!ipValidator.isValidInet6Address(value)) {
            return "should be a valid ipv6 address";
        }
        return null;
    }

    public static String uri(String field, String value)  {
        try {
            URI uri = new URI(value);
            if (!uri.isAbsolute()) {
                return "should be a valid absolute uri";
            }
        } catch (URISyntaxException ex) {
            return "should be a valid absolute uri";
        }
        return null;
    }

    public static String uriRef(String field, String value)  {
        try {
            new URI(value);
        } catch (URISyntaxException ex) {
            return "should be a valid absolute uri";
        }
        return null;
    }

    /**
     * Validates if the given value is a UUID or GUID in RFC 4122 hyphenated
     * ({@code 00000000-0000-0000-0000-000000000000}) form; both lower and upper
     * hex digits are accepted.
     */
    public static String uuid(final String field, final String value)  {
        final char[] chars = value.toCharArray();

        err: if (chars.length == UUID_LEN) {
            for (int i = 0; i < chars.length; i++) {
                final char c = chars[i];
                if (i == UUID_DASH_1 || i == UUID_DASH_2 || i == UUID_DASH_3 || i == UUID_DASH_4) {
                    if (c != '-') {
                        break err;
                    }
                } else if ((c < '0' || c > '9') && (c < 'a' || c > 'f') && (c < 'A' || c > 'F')) {
                    break err;
                }
            }
            return null;
        }

        return "invalid UUID string";
    }

    private static String enquote(String value) {
        return "\"" + value + "\"";
    }

    private static boolean isAscii(final String value) {
        for (char c : value.toCharArray()) {
            if (c > 127) {
                return false;
            }
        }
        return true;
    }
}
