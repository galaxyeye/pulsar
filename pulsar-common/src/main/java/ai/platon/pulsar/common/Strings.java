/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.platon.pulsar.common;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * A minimal String utility class. Designed for internal dom use only.
 *
 * @author vincent
 * @version $Id: $Id
 */
public final class Strings {

    /** Constant <code>emptyStringArray</code> */
    final public static String[] emptyStringArray = {};
    /** Constant <code>COMMA=','</code> */
    final public static char COMMA = ',';
    /** Constant <code>COMMA_STR=","</code> */
    final public static String COMMA_STR = ",";
    /** Constant <code>ESCAPE_CHAR='\\'</code> */
    final public static char ESCAPE_CHAR = '\\';

    // public static final Pattern HTML_CHARSET_PATTERN = Pattern.compile("^<meta(?!\\s*(?:name|value)\\s*=)(?:[^>]*?content\\s*=[\\s\"']*)?([^>]*?)[\\s\"';]*charset\\s*=[\\s\"']([a-zA-Z0-9]{3,8})([^\\s\"'(/>)]*)", CASE_INSENSITIVE);
    /** Constant <code>HTML_CHARSET_PATTERN</code> */
    public static final Pattern HTML_CHARSET_PATTERN = Pattern.compile("^<meta.+charset\\s*=[\\s\"']*([a-zA-Z0-9\\-]{3,8})[\\s\"'/>]*", CASE_INSENSITIVE);

    /** Constant <code>PRICE_PATTERN</code> */
    public static final Pattern PRICE_PATTERN = Pattern.compile("[1-9](,{0,1}\\d+){0,8}(\\.\\d{1,2})|[1-9](,{0,1}\\d+){0,8}");

    // all special chars on a standard keyboard
    /** Constant <code>DEFAULT_KEEP_CHARS="~!@#$%^&*()_+`-={}|[]\\:\";'<>?,./' \n\"{trunked}</code> */
    public static final String DEFAULT_KEEP_CHARS = "~!@#$%^&*()_+`-={}|[]\\:\";'<>?,./' \n\r\t";

    /** Constant <code>HTML_TAG_REGEX="<(\"[^\"]*\"|'[^']*'|[^'\">])*>"</code> */
    public static final String HTML_TAG_REGEX = "<(\"[^\"]*\"|'[^']*'|[^'\">])*>";

    /** Constant <code>FLOAT_REGEX="^([+-]?(\\d+\\.)?\\d+)$"</code> */
    public static final String FLOAT_REGEX = "^([+-]?(\\d+\\.)?\\d+)$";

    /** Constant <code>FLOAT_PATTERN</code> */
    public static Pattern FLOAT_PATTERN = Pattern.compile(FLOAT_REGEX);

    /** Constant <code>HTML_TAG_PATTERN</code> */
    public static Pattern HTML_TAG_PATTERN = Pattern.compile(HTML_TAG_REGEX);

    /** Constant <code>NUMERIC_LIKE_REGEX="^.{0,2}[-+]?[0-9]*\\.?[0-9]+.{0,2}$"</code> */
    public static final String NUMERIC_LIKE_REGEX = "^.{0,2}[-+]?[0-9]*\\.?[0-9]+.{0,2}$";

    /** Constant <code>NUMERIC_LIKE_PATTERN</code> */
    public static Pattern NUMERIC_LIKE_PATTERN = Pattern.compile(NUMERIC_LIKE_REGEX);

    /** Constant <code>MONEY_LIKE_REGEX="^[¥￥$]?[0-9]+(\\.[0-9]{1,2})?$"</code> */
    public static final String MONEY_LIKE_REGEX = "^[¥￥$]?[0-9]+(\\.[0-9]{1,2})?$";

    /** Constant <code>MONEY_LIKE_PATTERN</code> */
    public static Pattern MONEY_LIKE_PATTERN = Pattern.compile(MONEY_LIKE_REGEX);

    /** Constant <code>CHINESE_PHONE_NUMBER_LIKE_REGEX="^((13[0-9])|(14[5|7])|(15([0-3]|[5-9]))"{trunked}</code> */
    public static final String CHINESE_PHONE_NUMBER_LIKE_REGEX = "^((13[0-9])|(14[5|7])|(15([0-3]|[5-9]))|(18[0,1,2,5-9])|(177))\\d{8}$";

    /** Constant <code>CHINESE_PHONE_NUMBER_LIKE_PATTERN</code> */
    public static Pattern CHINESE_PHONE_NUMBER_LIKE_PATTERN = Pattern.compile(CHINESE_PHONE_NUMBER_LIKE_REGEX);

    /** Constant <code>IP_PORT_REGEX="^(((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A"{trunked}</code> */
    public static final String IP_PORT_REGEX = "^"
            + "(((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}" // Domain name
            + "|"
            + "localhost" // localhost
            + "|"
            + "(([0-9]{1,3}\\.){3})[0-9]{1,3})" // Ip
            + ":"
            + "[0-9]{1,5}$"; // Port

    /** Constant <code>IP_PORT_PATTERN</code> */
    public static final Pattern IP_PORT_PATTERN = Pattern.compile(IP_PORT_REGEX); // Port

    /** Constant <code>CODE_KEYBOARD_WHITESPACE=32</code> */
    public static final int CODE_KEYBOARD_WHITESPACE = 32;
    /** Constant <code>CODE_NBSP=160</code> */
    public static final int CODE_NBSP = 160;

    /** Constant <code>KEYBOARD_WHITESPACE="String.valueOf(CODE_KEYBOARD_WHITESPACE"{trunked}</code> */
    public static final String KEYBOARD_WHITESPACE = String.valueOf(CODE_KEYBOARD_WHITESPACE);
    // Html entity: {@code &nbsp;} looks just like a white space
    /** Constant <code>NBSP="String.valueOf(CODE_NBSP)"</code> */
    public static final String NBSP = String.valueOf(CODE_NBSP);

    /** Constant <code>HEX_DIGITS</code> */
    public static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
            'f'};

    // memoised padding up to 10
    /** Constant <code>padding</code> */
    public static final String[] padding = {"", " ", "  ", "   ", "    ", "     ", "      ", "       ", "        ",
            "         ", "          "};

    /** Constant <code>LongerFirstComparator</code> */
    public static final Comparator<String> LongerFirstComparator = (s, s2) -> {
        int result = Integer.compare(s2.length(), s.length());
        if (result == 0)
            return s.compareTo(s2);
        return result;
    };

    /** Constant <code>ShorterFirstComparator</code> */
    public static final Comparator<String> ShorterFirstComparator = (s, s2) -> LongerFirstComparator.compare(s2, s);

    /** Constant <code>PatternTime</code> */
    public static final Pattern PatternTime = Pattern.compile("[0-2][0-3]:[0-5][0-9]");

    /**
     * <p>countTimeString.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a int.
     */
    public static int countTimeString(String text) {
        Matcher matcher = PatternTime.matcher(text);
        int count = 0;
        while (matcher.find())
            count++;
        return count;
    }

    /**
     * Tests if a code point is "whitespace" as defined by what it looks like. Used for Element.text etc.
     *
     * @param c code point to test
     * @return true if code point is whitespace, false otherwise
     */
    public static boolean isActuallyWhitespace(int c) {
        return c == CODE_KEYBOARD_WHITESPACE || c == '\t' || c == '\n' || c == '\f' || c == '\r' || c == CODE_NBSP;
    }

    /**
     * Convenience call for {@link #toHexString(ByteBuffer, String, int)}, where
     * <code>sep = null; lineLen = Integer.MAX_VALUE</code>.
     *
     * @param buf The byte buffer
     * @return The hex string
     */
    public static String toHexString(ByteBuffer buf) {
        return toHexString(buf, null, Integer.MAX_VALUE);
    }

    /**
     * Get a text representation of a ByteBuffer as hexadecimal String, where each
     * pair of hexadecimal digits corresponds to consecutive bytes in the array.
     *
     * @param buf     input data
     * @param sep     separate every pair of hexadecimal digits with this separator, or
     *                null if no separation is needed.
     * @param lineLen break the output String into lines containing output for lineLen
     *                bytes.
     * @return The hex string
     */
    public static String toHexString(ByteBuffer buf, String sep, int lineLen) {
        return toHexString(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining(), sep, lineLen);
    }

    /**
     * Convenience call for {@link #toHexString(byte[], String, int)}, where
     * <code>sep = null; lineLen = Integer.MAX_VALUE</code>.
     *
     * @param buf the buffer
     * @return a {@link java.lang.String} object.
     */
    public static String toHexString(byte[] buf) {
        return toHexString(buf, null, Integer.MAX_VALUE);
    }

    /**
     * Get a text representation of a byte[] as hexadecimal String, where each
     * pair of hexadecimal digits corresponds to consecutive bytes in the array.
     *
     * @param buf     input data
     * @param sep     separate every pair of hexadecimal digits with this separator, or
     *                null if no separation is needed.
     * @param lineLen break the output String into lines containing output for lineLen
     *                bytes.
     * @return a {@link java.lang.String} object.
     */
    public static String toHexString(byte[] buf, String sep, int lineLen) {
        return toHexString(buf, 0, buf.length, sep, lineLen);
    }

    /**
     * Get a text representation of a byte[] as hexadecimal String, where each
     * pair of hexadecimal digits corresponds to consecutive bytes in the array.
     *
     * @param buf     input data
     * @param of      the offset into the byte[] to start reading
     * @param cb      the number of bytes to read from the byte[]
     * @param sep     separate every pair of hexadecimal digits with this separator, or
     *                null if no separation is needed.
     * @param lineLen break the output String into lines containing output for lineLen
     *                bytes.
     * @return a {@link java.lang.String} object.
     */
    public static String toHexString(byte[] buf, int of, int cb, String sep, int lineLen) {
        if (buf == null)
            return null;
        if (lineLen <= 0)
            lineLen = Integer.MAX_VALUE;
        StringBuffer res = new StringBuffer(cb * 2);
        for (int c = 0; c < cb; c++) {
            int b = buf[of++];
            res.append(HEX_DIGITS[(b >> 4) & 0xf]);
            res.append(HEX_DIGITS[b & 0xf]);
            if (c > 0 && (c % lineLen) == 0)
                res.append('\n');
            else if (sep != null && c < lineLen - 1)
                res.append(sep);
        }
        return res.toString();
    }

    // 根据Unicode编码完美的判断中文汉字和符号
    /**
     * <p>isChinese.</p>
     *
     * @param c a char.
     * @return a boolean.
     */
    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION;

    }

    // 完整的判断中文汉字和符号
    /**
     * <p>isChinese.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isChinese(String text) {
        char[] ch = text.toCharArray();
        for (int i = 0; i < ch.length; i++) {
            char c = ch[i];
            if (isChinese(c)) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>isMainlyChinese.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param percentage a double.
     * @return a boolean.
     */
    public static boolean isMainlyChinese(String text, double percentage) {
        if ("".equals(text)) return false;

        return 1.0 * countChinese(text) / text.length() >= percentage;
    }

    /**
     * <p>countChinese.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a int.
     */
    public static int countChinese(String text) {
        if ("".equals(text)) return 0;

        int count = 0;
        char[] ch = text.toCharArray();
        for (int i = 0; i < ch.length; i++) {
            if (isChinese(ch[i])) {
                ++count;
            }
        }

        return count;
    }

    // 只能判断部分CJK字符（CJK统一汉字）
    /**
     * <p>isChineseByREG.</p>
     *
     * @param str a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isChineseByREG(String str) {
        if (str == null) {
            return false;
        }

        final Pattern pattern = Pattern.compile("[\\u4E00-\\u9FBF]+");
        return pattern.matcher(str.trim()).find();
    }

    // 只能判断部分CJK字符（CJK统一汉字）
    /**
     * <p>isChineseCharByREG.</p>
     *
     * @param ch a char.
     * @return a boolean.
     */
    public static boolean isChineseCharByREG(char ch) {
        return ch >= '\u4E00' && ch <= '\u9FBF';
    }

    // 只能判断部分CJK字符（CJK统一汉字）
    /**
     * <p>isChineseByName.</p>
     *
     * @param str a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isChineseByName(String str) {
        if (str == null) {
            return false;
        }

        // 大小写不同：\\p 表示包含，\\P 表示不包含
        // \\p{Cn} 的意思为 Unicode 中未被定义字符的编码，\\P{Cn} 就表示 Unicode中已经被定义字符的编码
        String reg = "\\p{InCJK Unified Ideographs}&&\\P{Cn}";
        Pattern pattern = Pattern.compile(reg);
        return pattern.matcher(str.trim()).find();
    }

    // 对整个字符串：
    // 1. 仅保留英文字符、数字、汉字字符和keeps中的字符
    // 2. 去除网页空白：&nbsp;
    //
    // String attrName = "配 送 至：京 东 价：当&nbsp;当&nbsp;价";
    // attrName = StringUtils.strip(attrName).replaceAll("[\\s+:：(&nbsp;)]",
    // "");
    // the "blank" characters in the above phrase can not be stripped
    /**
     * <p>stripNonChar.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String stripNonChar(String text) {
        return stripNonChar(text, null);
    }

    /**
     * <p>stripNonChar.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param keeps a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String stripNonChar(String text, String keeps) {
        StringBuilder builder = new StringBuilder();

        if (keeps == null) {
            keeps = "";
        }

        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch) || isChineseCharByREG(ch)) {
                builder.append(ch);
            } else if (!keeps.equals("") && keeps.indexOf(ch) != -1) {
                builder.append(ch);
            }
        }

        return builder.toString();
    }

    /**
     * <p>trimNonChar.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String trimNonChar(String text) {
        return trimNonChar(text, null);
    }

    // 对字符串的头部和尾部：
    // 1. 仅保留英文字符、数字、汉字字符和keeps中的字符
    // 2. 去除网页空白：&nbsp;
    /**
     * <p>trimNonChar.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param keeps a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String trimNonChar(String text, String keeps) {
        int start = 0;
        int end = text.length();
        if (keeps == null)
            keeps = "";

        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch) || isChineseCharByREG(ch)
                    || keeps.indexOf(ch) != -1) {
                start = i;
                break;
            }
        }

        for (int i = text.length() - 1; i >= 0; --i) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch) || isChineseCharByREG(ch)
                    || keeps.indexOf(ch) != -1) {
                end = i + 1;
                break;
            }
        }

        return text.substring(start, end);
    }

    /**
     * <p>isCJK.</p>
     *
     * @param ch a char.
     * @return a boolean.
     */
    public static boolean isCJK(char ch) {
        return Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS;
    }

    // 对整个字符串：
    // 1. 仅保留英文字符、数字、汉字字符和keeps中的字符
    // 2. 去除网页空白：&nbsp;
    //
    // String attrName = "配 送 至：京 东 价：当&nbsp;当&nbsp;价";
    // attrName = StringUtils.strip(attrName).replaceAll("[\\s+:：(&nbsp;)]", "");
    // the "blank" characters in the above phrase can not be stripped
    /**
     * <p>stripNonCJKChar.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String stripNonCJKChar(String text) {
        return stripNonCJKChar(text, null);
    }

    /**
     * <p>stripNonCJKChar.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param keeps a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String stripNonCJKChar(String text, String keeps) {
        StringBuilder builder = new StringBuilder();

        if (keeps == null) {
            keeps = "";
        }

        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch) || isCJK(ch)) {
                builder.append(ch);
            } else if (!keeps.equals("") && keeps.indexOf(ch) != -1) {
                builder.append(ch);
            }
        }

        return builder.toString();
    }

    /**
     * <p>trimNonCJKChar.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String trimNonCJKChar(String text) {
        return trimNonCJKChar(text, null);
    }

    // 对字符串的头部和尾部：
    // 1. 仅保留英文字符、数字、汉字字符和keeps中的字符
    // 2. 去除网页空白：&nbsp;
    /**
     * <p>trimNonCJKChar.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param keeps a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String trimNonCJKChar(String text, String keeps) {
        int start = 0;
        int end = text.length();
        if (keeps == null)
            keeps = "";

        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch) || isCJK(ch) || keeps.indexOf(ch) != -1) {
                start = i;
                break;
            }
        }

        for (int i = text.length() - 1; i >= 0; --i) {
            char ch = text.charAt(i);
            if (Character.isLetterOrDigit(ch) || isCJK(ch) || keeps.indexOf(ch) != -1) {
                end = i + 1;
                break;
            }
        }

        return text.substring(start, end);
    }

    /**
     * <p>stripNonPrintableChar.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String stripNonPrintableChar(String text) {
        if (text == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        int len = text.length();
        for (int i = 0; i < len; ++i) {
            char ch = text.charAt(i);
            if (isActuallyWhitespace(ch)) {
                if (i > 0 && i < len - 1) {
                    builder.append(' ');
                }
                int j = i + 1;
                while (j < len && isActuallyWhitespace(text.charAt(j))) {
                    ++j;
                }
                i = j - 1;
            } else if (isPrintableUnicodeChar(ch)) {
                builder.append(ch);
            }
        }

        return builder.toString();
    }

    /**
     * <p>isPrintableUnicodeChar.</p>
     *
     * @link {https://stackoverflow.com/questions/220547/printable-char-in-java}
     * @param ch a char.
     * @return a boolean.
     */
    public static boolean isPrintableUnicodeChar(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return (!Character.isISOControl(ch)) && ch != KeyEvent.CHAR_UNDEFINED
                && block != null && block != Character.UnicodeBlock.SPECIALS;
    }

    /**
     * Copy from pulsar , remove "�"
     *
     * @param value the dirty String value.
     * @return clean String
     */
    public static String cleanField(String value) {
        value = value.replaceAll("�", "");
        return value;
    }

    /**
     * <p>getLongestPart.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param pattern a {@link java.util.regex.Pattern} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getLongestPart(final String text, final Pattern pattern) {
        String[] parts = pattern.split(text);

        if (parts.length == 1) {
            return "";
        }

        String longestPart = "";
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];

            if (p.length() > longestPart.length()) {
                longestPart = p;
            }
        }

        if (longestPart.length() == 0) {
            return "";
        } else {
            return longestPart.trim();
        }
    }

    /**
     * <p>getLongestPart.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param regex a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getLongestPart(final String text, final String regex) {
        return getLongestPart(text, Pattern.compile(regex));
    }

    /**
     * <p>csslize.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String csslize(String text) {
        text = StringUtils.uncapitalize(text).trim();
        text = StringUtils.join(text.split("(?=\\p{Upper})"), "-").toLowerCase();
        text = text.replaceAll("[-_]+", "-");
        text = text.replaceAll("\\s+", "-");

        return text;
    }

    // nav_top -> nav top, mainMenu -> main menu, image-detail -> image detail
    /**
     * <p>humanize.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String humanize(String text) {
        text = StringUtils.join(text.split("(?=\\p{Upper})"), " ");
        text = text.replaceAll("[-_]", " ").toLowerCase().trim();

        return text;
    }

    /**
     * <p>humanize.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param seperator a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String humanize(String text, String seperator) {
        text = StringUtils.join(text.split("(?=\\p{Upper})"), seperator);
        text = text.replaceAll("[-_]", seperator).toLowerCase().trim();

        return text;
    }

    /**
     * <p>humanize.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param suffix a {@link java.lang.String} object.
     * @param seperator a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String humanize(String text, String suffix, String seperator) {
        text = StringUtils.join(text.split("(?=\\p{Upper})"), seperator);
        text = text.replaceAll("[-_]", seperator).toLowerCase().trim();

        return text + seperator + suffix;
    }

    /**
     * <p>humanize.</p>
     *
     * @param clazz a {@link java.lang.Class} object.
     * @param suffix a {@link java.lang.String} object.
     * @param seperator a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String humanize(Class clazz, String suffix, String seperator) {
        String text = StringUtils.join(clazz.getSimpleName().split("(?=\\p{Upper})"), seperator);
        text = text.replaceAll("[-_]", seperator).toLowerCase().trim();

        return text + seperator + suffix;
    }

    /**
     * <p>getLeadingInteger.</p>
     *
     * @param s a {@link java.lang.String} object.
     * @param defaultValue a int.
     * @return a int.
     */
    public static int getLeadingInteger(String s, int defaultValue) {
        int numberEnd = StringUtils.lastIndexOfAny(s, "123456789");
        if (numberEnd == StringUtils.INDEX_NOT_FOUND) {
            return defaultValue;
        }
        return NumberUtils.toInt(s.substring(0, numberEnd), defaultValue);
    }

    /**
     * <p>getTailingInteger.</p>
     *
     * @param s a {@link java.lang.String} object.
     * @param defaultValue a int.
     * @return a int.
     */
    public static int getTailingInteger(String s, int defaultValue) {
        int numberStart = StringUtils.indexOfAny(s, "123456789");
        if (numberStart == StringUtils.INDEX_NOT_FOUND) {
            return defaultValue;
        }
        return NumberUtils.toInt(s.substring(numberStart), defaultValue);
    }

    /**
     * <p>getFirstInteger.</p>
     *
     * @param s The string that might contain a integer number.
     * @param defaultValue the default value if the string has no any integer number.
     * @return the extracted or the default integer number.
     */
    public static int getFirstInteger(String s, int defaultValue) {
        int numberStart = StringUtils.indexOfAny(s, "123456789");
        if (numberStart == StringUtils.INDEX_NOT_FOUND) {
            return defaultValue;
        }

        StringBuilder sb = new StringBuilder(s.length() - numberStart);
        int j = 0;
        for (int i = numberStart; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            } else if (c == ',' || c == '_') {
                // skip number delimiter, 12,345,678 or java style 12_345_678
                // nothing to do
            } else {
                break;
            }
        }

        return NumberUtils.toInt(sb.toString(), defaultValue);
    }

    /**
     * <p>getLastInteger.</p>
     *
     * TODO: do not support prefix +-
     *
     * @param s The string that might contain a integer number.
     * @param defaultValue the default value if the string has no any integer number.
     * @return the extracted or the default integer number.
     */
    public static int getLastInteger(String s, int defaultValue) {
        s = s.replaceAll("[,_]", "");
        s = StringUtils.reverse(s);
        Pattern pattern = Pattern.compile("[0-9]+");

        Matcher m = pattern.matcher(s);
        if (m.find()) {
            return NumberUtils.toInt(StringUtils.reverse(m.group()));
        }

        return defaultValue;
    }

    /**
     * <p>getFirstFloatNumber.</p>
     *
     * @param s The string that might contain a float number.
     * @param defaultValue the default value if the string has no any float number.
     * @return the extracted or the default float number.
     */
    public static float getFirstFloatNumber(String s, float defaultValue) {
        s = s.replaceAll("[,_]", "");
        Pattern pattern = Pattern.compile("[+-]?[0-9]*\\.?,?[0-9]+");

        Matcher m = pattern.matcher(s);
        if (m.find()) {
            return NumberUtils.toFloat(m.group());
        }

        return defaultValue;
    }

    /**
     * <p>getLastFloatNumber.</p>
     *
     * @param s The string that might contain a float number.
     * @param defaultValue the default value if the string has no any float number.
     * @return the extracted or the default float number.
     */
    public static float getLastFloatNumber(String s, float defaultValue) {
        s = s.replaceAll("[,_]", "");
        Pattern pattern = Pattern.compile("[+-]?[0-9]*\\.?,?[0-9]+");

        Matcher m = pattern.matcher(s);
        if (m.find()) {
            return NumberUtils.toFloat(m.group(m.groupCount()));
        }

        return defaultValue;
    }

    /**
     * <p>contains.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param searchCharSequences a {@link java.lang.CharSequence} object.
     * @return a boolean.
     */
    public static boolean contains(String text, CharSequence... searchCharSequences) {
        Validate.notNull(searchCharSequences);

        for (CharSequence search : searchCharSequences) {
            if (!text.contains(search)) return false;
        }

        return true;
    }

    /**
     * <p>containsAny.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param searchCharSequences a {@link java.lang.CharSequence} object.
     * @return a boolean.
     */
    public static boolean containsAny(String text, CharSequence... searchCharSequences) {
        Validate.notNull(searchCharSequences);

        for (CharSequence search : searchCharSequences) {
            if (text.contains(search)) return true;
        }

        return false;
    }

    /**
     * <p>containsNone.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @param searchCharSequences a {@link java.lang.CharSequence} object.
     * @return a boolean.
     */
    public static boolean containsNone(String text, CharSequence... searchCharSequences) {
        Validate.notNull(searchCharSequences);

        for (CharSequence search : searchCharSequences) {
            if (text.contains(search)) return false;
        }

        return true;
    }

    /**
     * <p>stringifyException.</p>
     *
     * @param e a {@link java.lang.Throwable} object.
     * @return a {@link java.lang.String} object.
     */
    public static String stringifyException(Throwable e) {
        Objects.requireNonNull(e);
        StringWriter stm = new StringWriter();
        PrintWriter wrt = new PrintWriter(stm);
        e.printStackTrace(wrt);
        wrt.close();
        return stm.toString();
    }

    /**
     * <p>simplifyException.</p>
     *
     * @param e a {@link java.lang.Throwable} object.
     * @return a {@link java.lang.String} object.
     */
    public static String simplifyException(@NotNull Throwable e) {
        String message = e.getMessage();
        if (message == null) {
            message = e.toString();
        }

        String[] lines = message.split("\n");
        int n = lines.length;
        if (n == 0) {
            return "";
        } else if (n == 1) {
            return lines[0];
        } else if (n == 2) {
            return lines[0] + "\t" + lines[1];
        } else {
            return lines[0] + "\t" + lines[1] + " ...";
        }
    }

    /**
     * <p>reverse.</p>
     *
     * @param s a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String reverse(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        StringBuilder sb = new StringBuilder(s);
        return sb.reverse().toString();
    }

    /**
     * <p>doubleQuoteIfContainsWhitespace.</p>
     *
     * @param s a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String doubleQuoteIfContainsWhitespace(String s) {
        if (StringUtils.containsWhitespace(s)) return "\"" + s + "\"";
        else return s;
    }

    /**
     * <p>readableBytes.</p>
     *
     * @param bytes a long.
     * @return a {@link java.lang.String} object.
     */
    public static String readableBytes(int bytes) {
        return readableBytes(bytes, -1, false);
    }

    /**
     * <p>readableBytes.</p>
     *
     * @param bytes a long.
     * @return a {@link java.lang.String} object.
     */
    public static String readableBytes(long bytes) {
        return readableBytes(bytes, -1, false);
    }

    /**
     * <p>readableBytes.</p>
     *
     * @param bytes a long.
     * @param si a boolean.
     * @return a {@link java.lang.String} object.
     */
    public static String readableBytes(int bytes, boolean si) {
        return readableBytes(bytes, -1, si);
    }

    /**
     * <p>readableBytes.</p>
     *
     * @param bytes a long.
     * @param si a boolean.
     * @return a {@link java.lang.String} object.
     */
    public static String readableBytes(long bytes, boolean si) {
        return readableBytes(bytes, -1, si);
    }

    /**
     * <p>readableBytes.</p>
     *
     * @param bytes a long.
     * @param scale a int.
     * @param si a boolean.
     * @return a {@link java.lang.String} object.
     */
    public static String readableBytes(int bytes, int scale, boolean si) {
        return readableBytes((long)bytes, scale, si);
    }

    /**
     * <p>readableBytes.</p>
     *
     * @param bytes a long.
     * @param scale a int.
     * @param si a boolean.
     * @return a {@link java.lang.String} object.
     */
    public static String readableBytes(long bytes, int scale, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        String format = scale > 0 ? "%," + scale + ".2f %sB" : "%,.2f %sB";
        return String.format(format, bytes / Math.pow(unit, exp), pre);
    }

    /**
     * Parse key-value pairs in a line, for example :
     * "a=1 b=2 c=3", "x:1 y:2 z:3"
     *
     * @param line The line to parse
     * @return The parsed result
     */
    public static Map<String, String> parseKvs(String line) {
        return parseKvs(line, "=");
    }

    /**
     * Parse key-value pairs in a line, for example :
     * "a=1 b=2 c=3", "x:1 y:2 z:3"
     *
     * @param line The line to parse
     * @return The parsed result
     * @param delimiter a {@link java.lang.String} object.
     */
    public static Map<String, String> parseKvs(String line, String delimiter) {
        return SParser.wrap(line).getKvs(delimiter);
    }

    /**
     * All lines separated by back slashes are merged together
     *
     * @param allLines All lines separated by "\n", some of them are separated by  back slash
     * @return All lines separated by back slashes are merged
     */
    public static List<String> getUnslashedLines(String allLines) {
        return mergeSlashedLines(Arrays.asList(allLines.split("\n")));
    }

    /**
     * <p>getUnslashedLines.</p>
     *
     * @param allLines a {@link java.lang.String} object.
     * @param EOL a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> getUnslashedLines(String allLines, String EOL) {
        return mergeSlashedLines(Arrays.asList(allLines.split(EOL)));
    }

    /**
     * Merge lines concatenated with back slash, for example :
     * The line "Java Stream API is very very usefull \\\n"
     * " and we use them everywhere "
     * can be merged into "Java Stream API is very very usefull and we use them everywhere"
     *
     * @param slashedLine a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String mergeSlashedLine(String slashedLine) {
        slashedLine = slashedLine.replaceAll("\n", "");
        slashedLine = slashedLine.replaceAll("\\\\", "");
        return slashedLine;
    }

    /**
     * All lines separated by back slashes are merged together
     *
     * @param linesWithSlash Lines with back slash
     * @return All lines separated by back slashes are merged
     */
    public static List<String> mergeSlashedLines(Iterable<String> linesWithSlash) {
        List<String> lines = new ArrayList<>();
        StringBuilder mergedLine = new StringBuilder();
        boolean merging;
        for (String line : linesWithSlash) {
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            if (line.endsWith("\\")) {
                merging = true;
                mergedLine.append(StringUtils.removeEnd(line, "\\"));
            } else {
                mergedLine.append(line);
                merging = false;
            }

            if (!merging) {
                if (mergedLine.length() > 0) {
                    lines.add(mergedLine.toString());
                    mergedLine = new StringBuilder();
                }
            }
        }

        return lines;
    }

    /**
     * <p>getLongestCommonSubstring.</p>
     *
     * @param a a {@link java.lang.String} object.
     * @param b a {@link java.lang.String} object.
     * @return a int.
     */
    public static int getLongestCommonSubstring(String a, String b){
        int m = a.length();
        int n = b.length();

        int max = 0;

        int[][] dp = new int[m][n];

        for(int i=0; i<m; i++){
            for(int j=0; j<n; j++){
                if(a.charAt(i) == b.charAt(j)){
                    if(i==0 || j==0){
                        dp[i][j]=1;
                    }else{
                        dp[i][j] = dp[i-1][j-1]+1;
                    }

                    if(max < dp[i][j])
                        max = dp[i][j];
                }

            }
        }

        return max;
    }

    /**
     * Not completely tested
     *
     * @param html a {@link java.lang.String} object.
     * @param charset a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String replaceCharsetInHtml(String html, String charset) {
        Matcher matcher = HTML_CHARSET_PATTERN.matcher(html);
        if (matcher.find()) {
            html = html.replaceAll(matcher.group(1), charset);
        }

        return html;
    }

    /**
     * Returns an arraylist of strings.
     *
     * @param str the comma seperated string values
     * @return the arraylist of the comma seperated string values
     */
    public static String[] getStrings(String str){
        Collection<String> values = getStringCollection(str);
        if(values.size() == 0) {
            return null;
        }
        return values.toArray(new String[0]);
    }

    /**
     * <p>arrayToString.</p>
     *
     * @param strs an array of {@link java.lang.String} objects.
     * @return a {@link java.lang.String} object.
     */
    public static String arrayToString(String[] strs) {
        if (strs.length == 0) { return ""; }
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(strs[0]);
        for (int idx = 1; idx < strs.length; idx++) {
            sbuf.append(",");
            sbuf.append(strs[idx]);
        }
        return sbuf.toString();
    }

    /**
     * Returns a collection of strings.
     *
     * @param str comma seperated string values
     * @return an <code>ArrayList</code> of string values
     */
    public static Collection<String> getStringCollection(String str){
        String delim = ",";
        return getStringCollection(str, delim);
    }

    /**
     * Returns a collection of strings.
     *
     * @param str
     *          String to parse
     * @param delim
     *          delimiter to separate the values
     * @return Collection of parsed elements.
     */
    public static Collection<String> getStringCollection(String str, String delim) {
        List<String> values = new ArrayList<String>();
        if (str == null)
            return values;
        StringTokenizer tokenizer = new StringTokenizer(str, delim);
        while (tokenizer.hasMoreTokens()) {
            values.add(tokenizer.nextToken());
        }
        return values;
    }

    /**
     * Splits a comma separated value <code>String</code>, trimming leading and trailing whitespace on each value.
     * Duplicate and empty values are removed.
     *
     * @param str a comma separated string with values
     * @return a <code>Collection</code> of <code>String</code> values
     */
    public static Collection<String> getTrimmedStringCollection(String str){
        Set<String> set = new LinkedHashSet<String>(Arrays.asList(getTrimmedStrings(str)));
        set.remove("");
        return set;
    }

    /**
     * Splits a comma separated value <code>String</code>, trimming leading and trailing whitespace on each value.
     *
     * @param str a comma separated String with values
     * @return an array of <code>String</code> values
     */
    public static String[] getTrimmedStrings(String str){
        if (null == str || str.trim().isEmpty()) {
            return emptyStringArray;
        }

        return str.trim().split("\\s*,\\s*");
    }

    /**
     * <p>hasHTMLTags.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean hasHTMLTags(String text) {
        Matcher matcher = HTML_TAG_PATTERN.matcher(text);
        return matcher.find();
    }

    /**
     * <p>isFloat.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isFloat(String text) {
        return FLOAT_PATTERN.matcher(text).matches();
    }

    /**
     * <p>isNumericLike.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isNumericLike(String text) {
        return NUMERIC_LIKE_PATTERN.matcher(text).matches();
    }

    /**
     * https://www.regextester.com/97725
     *
     * @param text a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isMoneyLike(String text) {
        return MONEY_LIKE_PATTERN.matcher(text).matches();
    }

    /**
     * <p>isIpPortLike.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isIpPortLike(String text) {
        return IP_PORT_PATTERN.matcher(text).matches();
    }

    /**
     * <p>isIpLike.</p>
     *
     * @param text a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isIpLike(String text) {
        return text.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    }
}
