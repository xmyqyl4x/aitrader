package com.myqyl.aitradex.util;

import java.math.BigDecimal;

/**
 * Utility class for price-related operations.
 */
public final class PriceUtils {

    private PriceUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Returns the first non-null value from the provided arguments.
     *
     * @param values varargs of BigDecimal values to check
     * @return the first non-null value, or null if all values are null
     */
    @SafeVarargs
    public static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Returns the first non-null BigDecimal value from the provided arguments.
     * This is a type-safe convenience method for BigDecimal values.
     *
     * @param values varargs of BigDecimal values to check
     * @return the first non-null value, or null if all values are null
     */
    public static BigDecimal firstAvailable(BigDecimal... values) {
        return firstNonNull(values);
    }
}
