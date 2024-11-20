package com.publiccms.common.tools;

import java.io.CharArrayWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import com.publiccms.common.constants.Constants;

/**
 * CommonUtils 通用Utils
 * 
 */
public class CommonUtils {

    private CommonUtils() {
    }

    private static BitSet dontNeedEncoding;
    private static final int caseDiff = ('a' - 'A');

    static {
        dontNeedEncoding = new BitSet(256);
        int i;
        for (i = 'a'; i <= 'z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = 'A'; i <= 'Z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = '0'; i <= '9'; i++) {
            dontNeedEncoding.set(i);
        }
        dontNeedEncoding.set(' ');
        dontNeedEncoding.set('-');
        dontNeedEncoding.set('_');
        dontNeedEncoding.set('.');
        dontNeedEncoding.set('*');
    }

    /**
     * @param <T>
     * @param <F>
     * @param list
     * @param keyMapper
     * @return
     */
    public static <T, F> Map<F, T> listToMap(List<T> list, Function<T, F> keyMapper) {
        return listToMap(list, keyMapper, null, null, null);
    }

    /**
     * @param <T>
     * @param <F>
     * @param list
     * @param keyMapper
     * @param sortKeys
     * @return
     */
    public static <T, F, K> Map<F, T> listToMapSorted(List<T> list, Function<T, F> keyMapper, K[] sortKeys) {
        return listToMapSorted(list, keyMapper, null, sortKeys, null);
    }

    /**
     * @param <T>
     * @param <F>
     * @param list
     * @param keyMapper
     * @param valueMapper
     * @param sortKeys
     * @param filter
     * @return
     */
    public static <T, F, K> Map<F, T> listToMapSorted(List<T> list, Function<T, F> keyMapper, UnaryOperator<T> valueMapper, K[] sortKeys, Predicate<T> filter) {
        return listToMap(list, keyMapper, valueMapper, (k1, k2) -> ArrayUtils.indexOf(sortKeys, k1) - ArrayUtils.indexOf(sortKeys, k2), filter);
    }

    /**
     * @param <T>
     * @param <F>
     * @param list
     * @param keyMapper
     * @param valueMapper
     * @param comparator
     * @param filter
     * @return
     */
    public static <T, F> Map<F, T> listToMap(List<T> list, Function<T, F> keyMapper, UnaryOperator<T> valueMapper, Comparator<? super T> comparator, Predicate<T> filter) {
        Stream<T> stream = list.stream();
        if (null != comparator) {
            stream = stream.sorted(comparator);
        }
        if (null != filter) {
            stream = stream.filter(filter);
        }
        return stream.collect(Collectors.toMap(keyMapper, null == valueMapper ? Function.identity() : valueMapper, Constants.defaultMegerFunction(), LinkedHashMap::new));
    }

    public static String encodeURI(String s) {
        boolean needToChange = false;
        StringBuilder out = new StringBuilder(s.length());
        CharArrayWriter charArrayWriter = new CharArrayWriter();
        for (int i = 0; i < s.length();) {
            int c = s.charAt(i);
            if (dontNeedEncoding.get(c)) {
                if (c == ' ') {
                    c = '+';
                    needToChange = true;
                }
                out.append((char) c);
                i++;
            } else {
                do {
                    charArrayWriter.write(c);
                    if (c >= 0xD800 && c <= 0xDBFF && (i + 1) < s.length()) {
                        int d = s.charAt(i + 1);
                        if (d >= 0xDC00 && d <= 0xDFFF) {
                            charArrayWriter.write(d);
                            i++;
                        }

                    }
                    i++;
                } while (i < s.length() && !dontNeedEncoding.get((c = s.charAt(i))));

                charArrayWriter.flush();
                String str = new String(charArrayWriter.toCharArray());
                byte[] ba = str.getBytes(StandardCharsets.UTF_8);
                for (int j = 0; j < ba.length; j++) {
                    out.append('%');
                    char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                    ch = Character.forDigit(ba[j] & 0xF, 16);
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                }
                charArrayWriter.reset();
                needToChange = true;
            }
        }
        return (needToChange ? out.toString() : s);
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
     * @param value
     * @return 是否为非空
     */
    public static boolean notEmpty(String value) {
        return StringUtils.isNotBlank(value);
    }

    /**
     * @param value
     * @return 是否为空
     */
    public static boolean empty(String value) {
        return StringUtils.isBlank(value);
    }

    /**
     * @param value
     * @return 是否非空
     */
    public static boolean notEmpty(Number value) {
        return null != value;
    }

    /**
     * @param value
     * @return 是否为空
     */
    public static boolean empty(Number value) {
        return null == value;
    }

    /**
     * @param value
     * @return 是否为空
     */
    public static boolean empty(Map<?, ?> value) {
        return null == value || value.isEmpty();
    }

    /**
     * @param value
     * @return 是否非空
     */
    public static boolean notEmpty(Collection<?> value) {
        return null != value && !value.isEmpty();
    }

    /**
     * @param value
     * @return 是否非空
     */
    public static boolean notEmpty(Map<?, ?> value) {
        return null != value && !value.isEmpty();
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
     * @param value
     * @return 是否非空
     */
    public static boolean notEmpty(String[] value) {
        if (null != value && 0 < value.length) {
            for (String t : value) {
                if (notEmpty(t)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param value
     * @return 是否非空
     */
    public static boolean notEmpty(Object[] value) {
        return null != value && 0 < value.length;
    }

    /**
     * @param value
     * @return 是否为空
     */
    public static boolean empty(Object[] value) {
        return null == value || 0 == value.length;
    }

    public static String getConfig(Properties config, String key) {
        return System.getProperty(key, config.getProperty(key));
    }
}
