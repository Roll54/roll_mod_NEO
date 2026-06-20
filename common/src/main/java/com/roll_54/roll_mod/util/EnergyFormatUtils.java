package com.roll_54.roll_mod.util;

import java.text.DecimalFormat;

public class EnergyFormatUtils {
    private static final String[] SUFFIXES = { "", "K", "M", "G", "T", "Q" };
    private static final long[] DIVISORS = {
            1L,
            1_000L,
            1_000_000L,
            1_000_000_000L,
            1_000_000_000_000L,
            1_000_000_000_000_000L
    };

    /**
     * Форматує число в короткий вигляд з точністю до тисячних.
     * Приклади:
     *  - 1500 -> 1.500K
     *  - 2_000_000 -> 2.000M
     *  - 12_500_000_000 -> 12.500G
     *  - 999 -> 999
     */
    public static String formatEnergy(long value) {
        if (Math.abs(value) < 1000) {
            return String.valueOf(value);
        }

        int index = 0;
        double display = value;

        while (index < SUFFIXES.length - 1 && Math.abs(display) >= 1000) {
            display /= 1000.0;
            index++;
        }

        // до трьох знаків після коми, без зайвих нулів вкінці
        DecimalFormat df = new DecimalFormat("#.###");
        return df.format(display) + SUFFIXES[index];
    }

    /** Повертає рядок типу "12.345M EU" */
    public static String formatEnergyWithUnit(long value) {
        return formatEnergy(value) + " EU";
    }
}