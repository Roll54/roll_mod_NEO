package com.roll_54.roll_mod.gtmogs.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

@SuppressWarnings("UnnecessaryUnicodeEscape")
public class FormattingUtil {

    /**
     * Does almost the same thing as {@code UPPER_CAMEL.to(LOWER_UNDERSCORE, string)},
     * but it also inserts underscores between words and numbers.
     *
     * @param string Any string with ASCII characters.
     * @return A string that is all lowercase, with underscores inserted before word/number boundaries:
     *
     *         <pre>
     *         <br>{@code "maragingSteel300" -> "maraging_steel_300"}
     *         <br>{@code "gtmogs:maraging_steel_300" -> "gtmogs:maraging_steel_300"}
     *         <br>{@code "maragingSteel_300" -> "maraging_steel_300"}
     *         <br>{@code "maragingSTEEL_300" -> "maraging_steel_300"}
     *         <br>{@code "MARAGING_STEEL_300" -> "maraging_steel_300"}
     * </pre>
     */
    public static String toLowerCaseUnderscore(String string) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char curChar = string.charAt(i);
            result.append(Character.toLowerCase(curChar));
            if (i == string.length() - 1) break;

            char nextChar = string.charAt(i + 1);
            if (curChar == '_' || nextChar == '_') continue;
            boolean nextIsUpper = Character.isUpperCase(nextChar);
            if (Character.isUpperCase(curChar) && nextIsUpper) continue;
            if (nextIsUpper || Character.isDigit(curChar) ^ Character.isDigit(nextChar)) result.append('_');
        }
        return result.toString();
    }

    /**
     * Check if {@code string} has any uppercase characters.
     *
     * @param string the string to check
     * @return if the string has any uppercase characters.
     */
    public static boolean hasUpperCase(String string) {
        for (int i = 0; i < string.length(); i++) {
            char ch = string.charAt(i);
            if (Character.isUpperCase(ch)) return true;
        }
        return false;
    }

    /**
     * apple_orange.juice => Apple Orange (Juice)
     */
    public static String toEnglishName(String internalName) {
        return Arrays.stream(internalName.toLowerCase(Locale.ROOT).split("_"))
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(" "));
    }
}
