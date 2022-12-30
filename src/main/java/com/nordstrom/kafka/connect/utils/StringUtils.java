package com.nordstrom.kafka.connect.utils;

public class StringUtils {
    /**
     * @see <a href="https://commons.apache.org/proper/commons-lang/apidocs/src-html/org/apache/commons/lang3/StringUtils.html#line.3571">StringUtils.isBlank()</>
     * @param input
     * @return true if input String is null, empty or blank.
     */
    public static boolean isBlank(String input) {
        return input == null || input.trim().isEmpty();
    }
}
