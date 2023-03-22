package com.publiccms.common.tools;

import java.io.File;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import com.publiccms.common.constants.Constants;

/**
 * 基类 Base
 * 
 */
public class CommonUtils {
    /**
     * @param <T>
     * @param <F>
     * @param list
     * @param keyMapper
     * @return
     */
    public static <T, F> Map<F, T> listToMap(List<T> list, Function<T, F> keyMapper) {
        return listToMap(list, keyMapper, null, null);
    }

    /**
     * @param <T>
     * @param <F>
     * @param list
     * @param keyMapper
     * @param consumer
     * @param filter
     * @return
     */
    public static <T, F> Map<F, T> listToMap(List<T> list, Function<T, F> keyMapper, Consumer<T> consumer, Predicate<T> filter) {
        if (null != consumer) {
            list.forEach(consumer);
        }
        Map<F, T> map;
        if (null == filter) {
            map = list.stream().collect(
                    Collectors.toMap(keyMapper, Function.identity(), Constants.defaultMegerFunction(), LinkedHashMap::new));
        } else {
            map = list.stream().filter(filter).collect(
                    Collectors.toMap(keyMapper, Function.identity(), Constants.defaultMegerFunction(), LinkedHashMap::new));
        }

        return map;
    }

    /**
     * @param elements
     * @return 拼接后的文本
     */
    @SafeVarargs
    public static <T> String joinString(T... elements) {
        if (null == elements || 0 == elements.length) {
            return null;
        } else if (1 == elements.length) {
            return String.valueOf(elements[0]);
        } else {
            StringBuilder sb = new StringBuilder();
            for (T e : elements) {
                if (null != e) {
                    sb.append(e);
                }
            }
            return sb.toString();
        }
    }

    /**
     * @param string
     * @param length
     * @return 截取后的文本
     */
    public static String keep(String string, int length) {
        return keep(string, length, "...");
    }

    /**
     * @param string
     * @param length
     * @param append
     * @return 截取后的文本
     */
    public static String keep(String string, int length, String append) {
        if (null == append) {
            return StringUtils.substring(string, 0, length);
        } else {
            if (null != string && string.length() > length) {
                if (length > append.length()) {
                    return joinString(string.substring(0, length - append.length()), append);
                } else {
                    return string.substring(0, length);
                }
            } else {
                return string;
            }
        }
    }

    /**
     * @return 当前日期
     */
    public static Date getDate() {
        return new Date();
    }

    /**
     * @return 精确到分钟的当前日期
     */
    public static Date getMinuteDate() {
        return DateUtils.ceiling(new Date(), Calendar.MINUTE);
    }

    /**
     * @param var
     * @return 是否为非空
     */
    public static boolean notEmpty(String var) {
        return StringUtils.isNotBlank(var);
    }

    /**
     * @param var
     * @return 是否为空
     */
    public static boolean empty(String var) {
        return StringUtils.isBlank(var);
    }

    /**
     * @param var
     * @return 是否非空
     */
    public static boolean notEmpty(Number var) {
        return null != var;
    }

    /**
     * @param var
     * @return 是否为空
     */
    public static boolean empty(Number var) {
        return null == var;
    }

    /**
     * @param var
     * @return 是否非空
     */
    public static boolean notEmpty(Collection<?> var) {
        return null != var && !var.isEmpty();
    }

    /**
     * @param var
     * @return 是否非空
     */
    public static boolean notEmpty(Map<?, ?> var) {
        return null != var && !var.isEmpty();
    }

    /**
     * @param file
     * @return 是否非空
     */
    public static boolean notEmpty(File file) {
        return null != file && file.exists();
    }

    /**
     * @param file
     * @return 是否为空
     */
    public static boolean empty(File file) {
        return null == file || !file.exists();
    }

    /**
     * @param var
     * @return 是否非空
     */
    public static boolean notEmpty(String[] var) {
        if (null != var && 0 < var.length) {
            for (String t : var) {
                if (notEmpty(t)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param var
     * @return 是否非空
     */
    public static boolean notEmpty(Object[] var) {
        return null != var && 0 < var.length;
    }

    /**
     * @param var
     * @return 是否为空
     */
    public static boolean empty(Object[] var) {
        return null == var || 0 == var.length;
    }

}
