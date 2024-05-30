package com.gitssie.openapi.rule;

import com.alibaba.fastjson.util.TypeUtils;
import com.google.common.collect.Maps;
import io.vavr.Lazy;
import io.vavr.Value;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.mozilla.javascript.*;
import org.mozilla.javascript.optimizer.OptRuntime;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * @author: Awesome
 * @create: 2024-02-04 17:18
 */
public class Rules {
    private static final Pattern ascii_pattern = Pattern.compile("^[\\x00-\\x7F]+");
    private static final Pattern email_pattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern phone_pattern = Pattern.compile("^1[3-9]\\d{9}$");

    private final Map<String, Object> rules = Maps.newHashMap();

    public Rules() {
        //all
        rules.put("required", Rules.required);
        rules.put("length", Rules.length);
        rules.put("same", Rules.same);
        //text
        rules.put("string", Rules.string);
        rules.put("base64", Rules.base64);
        rules.put("match", Rules.match);
        rules.put("ascii", Rules.ascii);
        rules.put("email", Rules.email);
        rules.put("phone", Rules.phone);

        //date
        rules.put("before", Rules.before);
        rules.put("after", Rules.after);
        rules.put("date", Rules.date);
        rules.put("dateformat", Rules.dateformat);

        //boolean
        rules.put("boolean", Rules.isBoolean);

        //number
        rules.put("between", Rules.between);
        rules.put("positive", Rules.positive);
        rules.put("integer", Rules.isInteger);
        rules.put("long", Rules.isLong);
        rules.put("number", Rules.number);
        rules.put("numeric", Rules.numeric);
        rules.put("max", Rules.max);
        rules.put("min", Rules.min);
        rules.put("range", Rules.range);

        //object
        rules.put("array", Rules.array);
        rules.put("object", Rules.object);

    }

    /**
     * AVAILABLE rules
     */

    public static final Rule required = new Rule("required", (value, field, model) -> {
        if (value instanceof String) {
            value = StringUtils.trim((String) value);
        }
        return ObjectUtils.isNotEmpty(value);
    });

    public static final RuleFunction length = new RuleFunction(e -> {
        Integer[] data = new Integer[2];
        data[0] = TypeUtils.castToInt(e[0]);
        data[1] = TypeUtils.castToInt(e.length > 1 ? e[1] : null);

        if (e.length == 1) {
            return new Rule("length", data, (value, field, model) -> {
                if (value == null) {
                    return true;
                }
                return size(value) >= data[0];
            });
        } else {
            return new Rule("length_between", data, (value, field, model) -> {
                if (value == null) {
                    return true;
                }
                int len = size(value);
                return len >= data[0] && len <= data[1];
            });
        }
    });

    /**
     * Checks if the value is after a given date string or `Date` object.
     */
    public static final RuleFunction after = new RuleFunction(e -> {
        Date[] data = new Date[]{TypeUtils.castToDate(e[0])};
        return new Rule("after", data, (value, field, model) -> {
            if (value == null) {
                return true;
            }
            Date date = parseDate(value);
            return date.after(data[0]);
        });
    });

    /**
     * Checks if a value is before a given date string or `Date` object.
     */
    public static final RuleFunction before = new RuleFunction(e -> {
        Date[] data = new Date[]{TypeUtils.castToDate(e[0])};
        return new Rule("before", data, (value, field, model) -> {
            if (value == null) {
                return true;
            }
            Date date = parseDate(value);
            return date.before(data[0]);
        });
    });

    /**
     * Checks if a value is between a given minimum or maximum, inclusive by default.
     */
    public static final RuleFunction between = new RuleFunction(e -> {
        Long[] data = new Long[]{parseDate(e[0]).getTime(), parseDate(e[1]).getTime()};
        boolean inclusive = false;
        if (e.length > 2) {
            inclusive = TypeUtils.castToBoolean(e[2]);
        }
        boolean inclusiveF = inclusive;
        return new Rule(inclusive ? "between_inclusive" : "between", data, (value, field, model) -> {
            if (value == null) {
                return true;
            }
            long date = parseDate(value).getTime();

            return inclusiveF ? date >= data[0] && date <= data[1] : date > data[0] && date < data[1];
        });
    });

    /**
     * Checks if a value is an array.
     */
    public static final Rule array = new Rule("array", (value, field, model) -> {
        if (value == null) {
            return true;
        }
        if (value.getClass().isArray()) {
            return true;
        } else if (value instanceof Collection) {
            return true;
        } else {
            return false;
        }
    });

    /**
     * Checks if a value equals another attribute's value.
     */
    public static final RuleFunction same = new RuleFunction(e -> {
        Object[] data = toArray(e[0]);
        return new Rule("same", data, (value, field, model) -> {
            if (value == null) {
                return true;
            }
            //@TODO 是数组如何处理
            return Objects.equals(value, data[0]);
        });
    });

    /**
     * Checks if a value equals the given value.
     */
    public static final RuleFunction equals = new RuleFunction(e -> {
        Object[] data = toArray(e[0]);
        return new Rule("equals", data, (value, field, model) -> {
            if (value == null) {
                return true;
            }
            return Objects.equals(value, data[0]);
        });
    });

    /**
     * Checks if a value is a number (integer or float), excluding `NaN`.
     */
    public static final Rule number = new Rule("number", (value, field, model) -> {
        return isNumber(value);
    });


    /**
     * Checks if a value is a number or numeric string, excluding `NaN`.
     */
    public static final Rule numeric = new Rule("numeric", (value, field, model) -> {
        return isNumber(value);
    });

    /**
     * Checks if a value is an integer.
     */
    public static final Rule isInteger = new Rule("integer", (value, field, model) -> {
        return isInteger(value);
    });

    /**
     * Checks if a value is an integer.
     */
    public static final Rule isLong = new Rule("long", (value, field, model) -> {
        return isLong(value);
    });

    /**
     * Checks if a value is a number or numeric string, excluding `NaN`.
     */
    public static final Rule string = new Rule("string", (value, field, model) -> {
        return value instanceof String;
    });


    /**
     * Checks if a value is an object, excluding arrays and functions.
     */
    public static final Rule object = new Rule("object", (value, field, model) -> {
        if (value == null) {
            return true;
        }
        if (value instanceof Map) {
            return true;
        } else {
            return false;
        }
    });

    /**
     * Checks if a value is positive.
     */
    public static final Rule positive = new Rule("positive", (value, field, model) -> {
        if (value == null) {
            return true;
        } else if (value instanceof Integer) {
            int nb = (int) value;
            return nb >= 0;
        } else if (value instanceof Long) {
            long nb = (long) value;
            return nb >= 0;
        } else if (value instanceof Number) {
            Number nb = (Number) value;
            return nb.longValue() >= 0;
        } else {
            return false;
        }
    });

    private static boolean isInteger(Object value) {
        if (value == null) {
            return true;
        } else if (value instanceof Integer) {
            return true;
        } else if (value instanceof String && StringUtils.isNumeric((String) value)) {
            Number n = NumberUtils.createNumber((String) value);
            double d = n.doubleValue();
            return (int) d == d;
        }

        return false;
    }

    private static boolean isLong(Object value) {
        if (value == null) {
            return true;
        } else if (value instanceof Integer || value instanceof Long) {
            return true;
        } else if (value instanceof String && StringUtils.isNumeric((String) value)) {
            Number n = NumberUtils.createNumber((String) value);
            double d = n.doubleValue();
            return (long) d == d;
        }
        return false;
    }

    private static boolean isNumber(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof Number) {
            return true;
        } else if (value.getClass().isArray()) {
            for (Object val : (Object[]) value) {
                if (!isNumber(val)) {
                    return false;
                }
            }
            return true;
        } else if (value instanceof String) {
            return NumberUtils.isParsable((String) value);
        } else {
            return false;
        }
    }

    private static Object[] toArray(Object val) {
        Object[] arg;
        if (val.getClass().isArray()) {
            arg = (Object[]) val;
        } else {
            arg = new Object[]{val};
        }
        return arg;
    }


    /**
     * Checks if a value is a string consisting only of ASCII characters.
     */
    public static final Rule ascii = new Rule("ascii", (value, field, model) -> {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence) {
            return ascii_pattern.matcher((CharSequence) value).matches();
        } else {
            return false;
        }
    });

    /**
     * Checks if a value is a valid Base64 string.
     */
    public static final Rule base64 = new Rule("base64", (value, field, model) -> {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return Base64.isBase64((String) value);
        } else {
            return false;
        }
    });

    /**
     * Checks if a value is a boolean (strictly true or false).
     */
    public static final Rule isBoolean = new Rule("boolean", (value, field, model) -> {
        if (value == null) {
            return true;
        }
        try {
            TypeUtils.castToBoolean(value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    });

    /**
     * Checks if a value is parseable as a date.
     */
    public static final Rule date = new Rule("date", (value, field, model) -> {
        if (value == null) {
            return true;
        }
        try {
            TypeUtils.castToDate(value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    });

    /**
     * Checks if a value is a valid email address.
     */
    public static final Rule email = new Rule("email", (value, field, model) -> isMatch(value, email_pattern));

    /**
     * Checks if a value is a valid chinese phone number.
     */
    public static final Rule phone = new Rule("phone", (value, field, model) -> isMatch(value, phone_pattern));


    /**
     * Checks if a value matches the given date format.
     */
    public static final RuleFunction dateformat = new RuleFunction(e -> {
        String[] pattern = new String[]{e[0].toString()};
        FastDateFormat df = FastDateFormat.getInstance(pattern[0]);
        return new Rule("dateformat", pattern, (value, field, model) -> {
            if (value == null) {
                return true;
            }
            if (value instanceof String) {
                try {
                    df.parse((String) value);
                    return true;
                } catch (ParseException ex) {
                    return false;
                }
            } else {
                return false;
            }
        });
    });

    private static boolean isMatch(Object value, Pattern pattern) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return pattern.matcher((String) value).matches();
        } else {
            return false;
        }
    }

    /**
     * Checks if a value matches a given regular expression string or RegExp.
     */
    public static final RuleFunction match = new RuleFunction(e -> {
        String[] pattern = new String[]{e[0].toString()};
        Pattern pt = Pattern.compile(pattern[0]);
        return new Rule("match", pattern, (value, field, model) -> isMatch(value, pt));
    });

    public static final RuleFunction max = new RuleFunction(e -> {
        double maxValue = ((Number) e[0]).doubleValue();
        return new Rule("max", (value, field, model) -> {
            if (value == null) {
                return true;
            }
            try {
                double input = TypeUtils.castToDouble(value);
                return input <= maxValue;
            } catch (Exception ex) {
                return false;
            }
        });
    });

    public static final RuleFunction min = new RuleFunction(e -> {
        double minValue = ((Number) e[0]).doubleValue();
        return new Rule("min", (value, field, model) -> {
            if (value == null) {
                return true;
            }
            try {
                double input = TypeUtils.castToDouble(value);
                return input >= minValue;
            } catch (Exception ex) {
                return false;
            }
        });
    });

    //number range
    public static final RuleFunction range = new RuleFunction(e -> {
        double minValue = ((Number) e[0]).doubleValue();
        double maxValue = ((Number) e[1]).doubleValue();
        boolean inclusive = true;
        if (e.length > 2) {
            inclusive = TypeUtils.castToBoolean(e[2]);
        }
        boolean inclusiveF = inclusive;
        return new Rule(inclusive ? "range_inclusive" : "range", (value, field, model) -> {
            if (value == null) {
                return true;
            }
            try {
                double input = TypeUtils.castToDouble(value);
                if (inclusiveF) {
                    return input >= minValue && input <= maxValue;
                } else {
                    return input > minValue && input < maxValue;
                }
            } catch (Exception ex) {
                return false;
            }
        });
    });

    public static final FormatFunction format = new FormatFunction();
    public static final ParseFunction parse = new ParseFunction();
    public static final ParseDateFunction parseDateFunction = new ParseDateFunction();

    public static int size(Object value) {
        if (value == null) {
            return 0;
        } else if (value instanceof CharSequence) {
            return StringUtils.length((CharSequence) value);
        } else if (value.getClass().isArray()) {
            return Array.getLength(value);
        } else if (value instanceof Collection) {
            return ((Collection<?>) value).size();
        } else {
            return 0;
        }
    }

    public static Date parseDate(Object value) {
        Date date;
        if (value instanceof Number) {
            date = TypeUtils.castToDate(((Number) value).longValue());
        } else {
            date = TypeUtils.castToDate(value);
        }
        return date;
    }

    public static Lazy<Context> context() {
        return Lazy.of(() -> Context.enter());
    }

    public static <T> T toValue(Function<Value<Context>, T> func) {
        Lazy<Context> context = Lazy.of(() -> Context.enter());
        try {
            return func.apply(context);
        } finally {
            if (context.isEvaluated()) {
                context.get().close();
            }
        }
    }

    public static <T> T toValue(Value<Context> contextOpt, Class<T> clazz, Object value, Object... args) {
        if (value instanceof org.mozilla.javascript.Function) {
            value = runFunction(contextOpt, clazz, value, args);
        }
        if (value == null || value instanceof Undefined) {
            return null;
        } else if (clazz == null) {
            return (T) value;
        } else {
            return TypeUtils.cast(value, clazz, null);
        }
    }

    public static Object toValue(Value<Context> contextOpt, Object value, Object... args) {
        if (value instanceof org.mozilla.javascript.Function) {
            value = runFunction(contextOpt, value, args);
        }
        if (value == null || value instanceof Undefined) {
            return null;
        }
        return value;
    }

    public static boolean isFunction(Object value) {
        return value != null && value instanceof org.mozilla.javascript.Function;
    }

    public static <T> T runFunction(Value<Context> contextOpt, Object function, Object... args) {
        return runFunction(contextOpt, null, function, args);
    }

    public static <T> T runFunction(Value<Context> contextOpt, Class<T> clazz, Object function, Object... args) {
        return runFunction(contextOpt, new NativeObject(), clazz, function, args);
    }

    public static <T> T runFunction(Value<Context> contextOpt, Scriptable scope, Class<T> clazz, Object function, Object... args) {
        Context context = contextOpt.isEmpty() ? Context.enter() : contextOpt.get();
        org.mozilla.javascript.Function func = (org.mozilla.javascript.Function) function;
        try {
            Object result = func.call(context, scope, null, args);
            if (result == null || result instanceof Undefined) {
                return null;
            }
            if (clazz != null) {
                return (T) Context.jsToJava(result, clazz);
            } else {
                return (T) result;
            }
        } finally {
            if (contextOpt.isEmpty()) {
                IOUtils.closeQuietly(context);
            }
        }
    }

    public static boolean isDate(Object value) {
        if (value instanceof Date) {
            return true;
        } else if (value instanceof Scriptable) {
            String type = ((Scriptable) value).getClassName();
            if (StringUtils.equals(type, "Date")) {
                return true;
            }
        }
        return false;
    }

    public static Date castToDate(Object value) {
        if (value instanceof Date) {
            return (Date) value;
        } else if (value instanceof Scriptable) {
            String type = ((Scriptable) value).getClassName();
            if (StringUtils.equals(type, "Date")) {
                return (Date) Context.jsToJava(value, Date.class);
            }
        }
        return TypeUtils.castToDate(value);
    }


    public Map<String, Object> getRules() {
        return rules;
    }

    public static Rules INSTANCE = new Rules();
}
