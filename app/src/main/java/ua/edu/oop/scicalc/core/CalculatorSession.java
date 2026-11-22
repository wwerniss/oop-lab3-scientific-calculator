package ua.edu.oop.scicalc.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Testable controller for keypad actions, answer memory and calculation history.
 * Android-only concerns (translations, views, preferences) stay in MainActivity.
 */
public final class CalculatorSession {
    public static final int MAX_HISTORY = 12;
    private String expression = "";
    private String result = "0";
    private AngleMode mode = AngleMode.DEGREES;
    private double lastAnswer;
    private boolean justEvaluated;
    private CalculationException.Type error;
    private final List<HistoryEntry> history = new ArrayList<>();

    public void press(String key) {
        if (key == null) return;
        switch (key) {
            case "AC": clear(); return;
            case "DEL": delete(); return;
            case "=": calculate(); return;
            case "DEG":
            case "RAD": toggleAngleMode(); return;
            default: append(key);
        }
    }

    public void clear() {
        expression = "";
        result = "0";
        error = null;
        justEvaluated = false;
    }

    public void delete() {
        error = null;
        if (justEvaluated) {
            expression = result.equals("0") ? "" : result;
            justEvaluated = false;
        }
        if (!expression.isEmpty()) {
            // Delete an inserted function token as a unit.
            boolean deleted = false;
            for (String token : new String[]{"sqrt(", "sin(", "cos(", "tan(", "log(", "abs(", "Ans", "ln(", "√("}) {
                if (expression.endsWith(token)) {
                    expression = expression.substring(0, expression.length() - token.length());
                    deleted = true;
                    break;
                }
            }
            if (!deleted) expression = expression.substring(0, expression.length() - 1);
        }
        updatePreview();
    }

    private void append(String key) {
        String token;
        switch (key) {
            case "sin": case "cos": case "tan": case "ln": case "log": case "abs":
                token = key + "("; break;
            case "√": token = "√("; break;
            case "x²": token = "^2"; break;
            case "xʸ": token = "^"; break;
            case "1/x": token = "^(-1)"; break;
            default: token = key;
        }
        // Continue with last result only for operations; start new otherwise.
        if (justEvaluated) {
            expression = isContinuation(token) ? result : "";
            justEvaluated = false;
        }
        if (expression.length() + token.length() > 300) {
            error = CalculationException.Type.COMPLEXITY;
            return;
        }
        error = null;
        expression += token;
        updatePreview();
    }

    private static boolean isContinuation(String token) {
        return token.equals("+") || token.equals("−") || token.equals("-")
                || token.equals("×") || token.equals("÷") || token.equals("^")
                || token.equals("^2") || token.equals("^(-1)")
                || token.equals("!") || token.equals("%");
    }

    private void updatePreview() {
        if (expression.isEmpty()) { result = "0"; return; }
        try {
            result = ResultFormatter.format(ExpressionEvaluator.evaluate(expression, mode, lastAnswer));
        } catch (CalculationException ex) {
            result = "…"; // Incomplete expressions are normal while typing.
        }
    }

    public boolean calculate() {
        if (expression.isEmpty()) return false;
        try {
            double computed = ExpressionEvaluator.evaluate(expression, mode, lastAnswer);
            String formatted = ResultFormatter.format(computed);
            lastAnswer = computed;
            result = formatted;
            error = null;
            justEvaluated = true;
            history.add(0, new HistoryEntry(expression, formatted));
            if (history.size() > MAX_HISTORY) history.remove(history.size() - 1);
            return true;
        } catch (CalculationException ex) {
            error = ex.getType();
            justEvaluated = false;
            return false;
        }
    }

    public void toggleAngleMode() {
        mode = mode == AngleMode.DEGREES ? AngleMode.RADIANS : AngleMode.DEGREES;
        error = null;
        if (!justEvaluated) updatePreview();
    }

    public String getExpression() { return expression; }
    public String getResult() { return result; }
    public AngleMode getAngleMode() { return mode; }
    public double getLastAnswer() { return lastAnswer; }
    public CalculationException.Type getError() { return error; }
    public boolean isJustEvaluated() { return justEvaluated; }
    public List<HistoryEntry> getHistory() { return Collections.unmodifiableList(history); }

    public void clearHistory() { history.clear(); }

    public void restore(String expression, String result, double lastAnswer, AngleMode mode, boolean evaluated) {
        this.expression = expression == null ? "" : expression;
        this.result = result == null ? "0" : result;
        this.lastAnswer = Double.isFinite(lastAnswer) ? lastAnswer : 0;
        this.mode = mode == null ? AngleMode.DEGREES : mode;
        this.justEvaluated = evaluated;
        this.error = null;
    }

    public void restoreHistory(List<HistoryEntry> entries) {
        history.clear();
        if (entries == null) return;
        for (HistoryEntry entry : entries) {
            if (entry != null && history.size() < MAX_HISTORY) history.add(entry);
        }
    }
}
