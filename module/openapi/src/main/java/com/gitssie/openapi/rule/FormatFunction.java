package com.gitssie.openapi.rule;

import com.gitssie.openapi.utils.Libs;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import javax.validation.Valid;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

/**
 * @author: Awesome
 * @create: 2024-02-27 09:18
 */
public class FormatFunction extends BaseFunction {
    private Pattern number = Pattern.compile("^\\d+\\.?\\d*");
    private Pattern date = Pattern.compile("^y{2,4}.+");
    private Map<String, DecimalFormat> numberFormatCache = new WeakHashMap<>();

    @Override
    public Object call(Context ctx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length == 0) {
            return null;
        } else if (args.length == 1) {
            return format(args[0], "");
        } else {
            return format(args[1], (String) args[0]);
        }
    }

    /**
     * 数据格式化
     *
     * @param value
     * @param format
     * @return
     */
    public Object format(Object value, String format) {
        if (StringUtils.equalsIgnoreCase("date", format)) {
            format = Libs.DATE_FORMAT;
        } else if (StringUtils.equalsIgnoreCase("time", format)) {
            format = "HH:mm:ss";
        } else if (StringUtils.equalsIgnoreCase("datetime", format)) {
            format = Libs.DATETIME_FORMAT;
        }

        if (number.matcher(format).matches()) {
            if (value == null) {
                value = 0;
            }
            DecimalFormat df = numberFormatCache.computeIfAbsent(format, (e) -> new DecimalFormat(e));
            return df.format(value);
        } else if (date.matcher(format).matches()) {
            if (value == null) {
                return null;
            }
            if (value instanceof Long) {
                long a = ((Long) value).longValue();
                int b = ((Long) value).intValue();
                if (a == b) {
                    a *= 1000;
                }
                FastDateFormat df = FastDateFormat.getInstance(format);
                return df.format(new Date(a));
            } else if (value instanceof Integer) {
                FastDateFormat df = FastDateFormat.getInstance(format);
                return df.format(new Date((Integer) value * 1000l));
            }
        } else if (value instanceof String) {
            return MessageFormat.format(format, value);
        }

        return formatDate(value, format);
    }

    public Object formatDate(Object value, String format) {
        format = StringUtils.defaultIfBlank(format, Libs.DATETIME_FORMAT);
        if (value instanceof Date) {
            return FastDateFormat.getInstance(format).format(value);
        } else if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(DateTimeFormatter.ofPattern(format));
        } else if (value instanceof Scriptable) {
            String type = ((Scriptable) value).getClassName();
            if (StringUtils.equals(type, "Date")) {
                return FastDateFormat.getInstance(format).format(Context.jsToJava(value, Date.class));
            }
        }
        return value;
    }
}
