package ua.edu.oop.scicalc.core;

import java.util.Objects;

/** Immutable history entry, independent from the Android framework. */
public final class HistoryEntry {
    private final String expression;
    private final String result;

    public HistoryEntry(String expression, String result) {
        this.expression = Objects.requireNonNull(expression);
        this.result = Objects.requireNonNull(result);
    }
    public String getExpression() { return expression; }
    public String getResult() { return result; }
    public String asLine() { return expression + " = " + result; }
}
