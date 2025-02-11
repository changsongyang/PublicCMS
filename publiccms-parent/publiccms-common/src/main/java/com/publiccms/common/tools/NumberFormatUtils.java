package com.publiccms.common.tools;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * DateFormatUtil
 *
 */
public class NumberFormatUtils {
    private NumberFormatUtils() {
    }

    private static ThreadLocal<Map<String, DecimalFormat>> threadLocal = new ThreadLocal<>();

    /**
     * short date format
     */
    public static final String NORMAL_FORMAT_STRING = "0";

    /**
     * @param pattern
     * @return number format
     */
    public static DecimalFormat getFormat(String pattern) {
        Map<String, DecimalFormat> map = threadLocal.get();
        DecimalFormat format = null;
        if (null == map) {
            map = new HashMap<>();
            format = new DecimalFormat(pattern);
            map.put(pattern, format);
            threadLocal.set(map);
        } else {
            format = map.computeIfAbsent(pattern, DecimalFormat::new);
        }
        return format;
    }
}
