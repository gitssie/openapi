package com.gitssie.openapi.utils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

public class TypeUtils {

    public static Double castToDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            String strVal = value.toString();
            if (strVal.length() == 0 //
                    || "null".equals(strVal) //
                    || "NULL".equals(strVal)) {
                return null;
            }
            if (strVal.indexOf(',') != -1) {
                strVal = strVal.replaceAll(",", "");
            }
            return Double.parseDouble(strVal);
        }

        if (value instanceof Boolean) {
            return (Boolean) value ? 1D : 0D;
        }

        throw new IllegalArgumentException("can not cast to double, value : " + value);
    }

    public static Integer castToInt(Object value) {
        return castToInt(value, null);
    }

    public static Integer castToInt(Object value, Integer dft) {
        if (value == null) {
            return dft;
        } else if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof BigDecimal) {
            return intValue((BigDecimal) value);
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            String strVal = (String) value;
            if (strVal.length() != 0 && !"null".equals(strVal) && !"NULL".equals(strVal)) {
                if (strVal.indexOf(44) != 0) {
                    strVal = strVal.replaceAll(",", "");
                }

                return Integer.parseInt(strVal);
            } else {
                return dft;
            }
        } else if (value instanceof Boolean) {
            return (Boolean) value ? 1 : 0;
        } else {
            if (value instanceof Map) {
                Map map = (Map) value;
                if (map.size() == 2 && map.containsKey("andIncrement") && map.containsKey("andDecrement")) {
                    Iterator iter = map.values().iterator();
                    iter.next();
                    Object value2 = iter.next();
                    return castToInt(value2);
                }
            }

            throw new IllegalArgumentException("can not cast to int, value : " + value);
        }
    }

    public static short shortValue(BigDecimal decimal) {
        if (decimal == null) {
            return 0;
        } else {
            int scale = decimal.scale();
            return scale >= -100 && scale <= 100 ? decimal.shortValue() : decimal.shortValueExact();
        }
    }

    public static int intValue(BigDecimal decimal) {
        if (decimal == null) {
            return 0;
        } else {
            int scale = decimal.scale();
            return scale >= -100 && scale <= 100 ? decimal.intValue() : decimal.intValueExact();
        }
    }

    public static long longValue(BigDecimal decimal) {
        if (decimal == null) {
            return 0L;
        } else {
            int scale = decimal.scale();
            return scale >= -100 && scale <= 100 ? decimal.longValue() : decimal.longValueExact();
        }
    }

    public static Long castToLong(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof BigDecimal) {
            return longValue((BigDecimal) value);
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else {
            if (value instanceof String) {
                String strVal = (String) value;
                if (strVal.length() == 0 || "null".equals(strVal) || "NULL".equals(strVal)) {
                    return null;
                }

                if (strVal.indexOf(44) != 0) {
                    strVal = strVal.replaceAll(",", "");
                }

                try {
                    return Long.parseLong(strVal);
                } catch (NumberFormatException var4) {
                    throw new IllegalArgumentException(var4);
                }
            }

            if (value instanceof Map) {
                Map map = (Map) value;
                if (map.size() == 2 && map.containsKey("andIncrement") && map.containsKey("andDecrement")) {
                    Iterator iter = map.values().iterator();
                    iter.next();
                    Object value2 = iter.next();
                    return castToLong(value2);
                }
            }

            throw new IllegalArgumentException("can not cast to long, value : " + value);
        }
    }

    public static Boolean castToBoolean(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof BigDecimal) {
            return intValue((BigDecimal) value) == 1;
        } else if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        } else if (value instanceof String) {
            String strVal = (String) value;
            if (strVal.length() != 0 && !"null".equals(strVal) && !"NULL".equals(strVal)) {
                if (!"true".equalsIgnoreCase(strVal) && !"1".equals(strVal)) {
                    if (!"false".equalsIgnoreCase(strVal) && !"0".equals(strVal)) {
                        if (!"Y".equalsIgnoreCase(strVal) && !"T".equals(strVal)) {
                            if (!"F".equalsIgnoreCase(strVal) && !"N".equals(strVal)) {
                                throw new IllegalArgumentException("can not cast to boolean, value : " + value);
                            } else {
                                return Boolean.FALSE;
                            }
                        } else {
                            return Boolean.TRUE;
                        }
                    } else {
                        return Boolean.FALSE;
                    }
                } else {
                    return Boolean.TRUE;
                }
            } else {
                return null;
            }
        } else {
            throw new IllegalArgumentException("can not cast to boolean, value : " + value);
        }
    }

    public static Date castToDate(Object value) {
        return castToDate(value, null);
    }

    public static Date castToDate(Object value, String format) {
        if (value == null) {
            return null;
        }

        if (value instanceof Date) { // 使用频率最高的，应优先处理
            return (Date) value;
        }

        if (value instanceof Calendar) {
            return ((Calendar) value).getTime();
        }

        long longValue = -1;

        if (value instanceof BigDecimal) {
            longValue = longValue((BigDecimal) value);
            return new Date(longValue);
        }

        if (value instanceof Number) {
            longValue = ((Number) value).longValue();
            if ("unixtime".equals(format)) {
                longValue *= 1000;
            }
            return new Date(longValue);
        }

        if (value instanceof String) {
            String strVal = (String) value;
            if (strVal.startsWith("/Date(") && strVal.endsWith(")/")) {
                strVal = strVal.substring(6, strVal.length() - 2);
            }
            if (strVal.indexOf('-') > 0 || strVal.indexOf('+') > 0 || format != null) {
                if (format == null) {
                    final int len = strVal.length();
                    if (len == 10) {
                        format = "yyyy-MM-dd";
                    } else if (len == "yyyy-MM-dd HH:mm".length()) {
                        format = "yyyy-MM-dd HH:mm";
                    } else if (len == "yyyy-MM-dd HH:mm:ss".length()) {
                        format = "yyyy-MM-dd HH:mm:ss";
                    } else if (len == 29
                            && strVal.charAt(26) == ':'
                            && strVal.charAt(28) == '0') {
                        format = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
                    } else if (len == 23 && strVal.charAt(19) == ',') {
                        format = "yyyy-MM-dd HH:mm:ss,SSS";
                    } else {
                        format = "yyyy-MM-dd HH:mm:ss.SSS";
                    }
                }

                SimpleDateFormat dateFormat = new SimpleDateFormat(format);
                try {
                    return dateFormat.parse(strVal);
                } catch (ParseException e) {
                    throw new IllegalArgumentException("can not cast to Date, value : " + strVal);
                }
            }
            if (strVal.length() == 0) {
                return null;
            }
            longValue = Long.parseLong(strVal);
        }

        return new Date(longValue);
    }

    public static String castToString(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return (String) value;
        } else {
            return value.toString();
        }
    }
}
