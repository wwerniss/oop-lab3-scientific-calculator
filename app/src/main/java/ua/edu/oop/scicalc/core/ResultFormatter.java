package ua.edu.oop.scicalc.core;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Uses a stable decimal dot regardless of the system language. */
public final class ResultFormatter {
    private ResultFormatter() {}

    public static String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new CalculationException(CalculationException.Type.OVERFLOW);
        }
        if (value == 0.0 || Math.abs(value) < 1e-14) return "0";
        BigDecimal rounded = BigDecimal.valueOf(value)
                .round(new MathContext(12, RoundingMode.HALF_UP))
                .stripTrailingZeros();
        double abs = Math.abs(value);
        return abs >= 1e12 || abs < 1e-7
                ? rounded.toString()
                : rounded.toPlainString();
    }
}
