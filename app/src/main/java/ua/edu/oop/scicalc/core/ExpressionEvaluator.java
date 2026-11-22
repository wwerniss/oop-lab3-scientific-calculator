package ua.edu.oop.scicalc.core;

import java.util.Locale;

import static ua.edu.oop.scicalc.core.CalculationException.Type.*;

/**
 * Dependency-free recursive-descent expression evaluator.
 *
 * Precedence, low to high: + -, multiplication/division (incl. implicit),
 * unary +/-, power (right-associative), postfix ! %, primary/function.
 * Examples: -2^2=-4; 2^3^2=512; 3sin(30)=1.5 in DEG mode.
 */
public final class ExpressionEvaluator {
    private static final int MAX_LENGTH = 300;
    private static final int MAX_DEPTH = 64;
    private final String source;
    private final AngleMode mode;
    private final double answer;
    private int pos;
    private int depth;

    private ExpressionEvaluator(String source, AngleMode mode, double answer) {
        this.source = source;
        this.mode = mode;
        this.answer = answer;
    }

    public static double evaluate(String expression, AngleMode mode, double lastAnswer) {
        if (expression == null || mode == null || expression.trim().isEmpty()) {
            throw new CalculationException(SYNTAX);
        }
        if (expression.length() > MAX_LENGTH) {
            throw new CalculationException(COMPLEXITY);
        }
        ExpressionEvaluator parser = new ExpressionEvaluator(expression, mode, lastAnswer);
        double value = parser.expression();
        parser.skipSpaces();
        if (parser.pos != expression.length()) throw new CalculationException(SYNTAX);
        return finite(value);
    }

    private double expression() {
        enter();
        try {
            double v = term();
            while (true) {
                if (eat('+')) v = finite(v + term());
                else if (eat('-') || eat('−')) v = finite(v - term());
                else return v;
            }
        } finally { leave(); }
    }

    private double term() {
        double v = unary();
        while (true) {
            if (eat('*') || eat('×')) v = finite(v * unary());
            else if (eat('/') || eat('÷')) {
                double divisor = unary();
                if (divisor == 0.0) throw new CalculationException(DIVISION_BY_ZERO);
                v = finite(v / divisor);
            } else if (canImplicitMultiply()) {
                v = finite(v * unary());
            } else return v;
        }
    }

    private boolean canImplicitMultiply() {
        skipSpaces();
        if (pos == source.length()) return false;
        char c = source.charAt(pos);
        return c == '(' || c == 'π' || Character.isLetter(c) || Character.isDigit(c);
    }

    private double unary() {
        if (eat('+')) return unary();
        if (eat('-') || eat('−')) return finite(-unary());
        return power();
    }

    private double power() {
        double base = postfix();
        if (eat('^')) return finite(Math.pow(base, unary()));
        return base;
    }

    private double postfix() {
        double value = primary();
        while (true) {
            if (eat('!')) value = factorial(value);
            else if (eat('%')) value = finite(value / 100.0);
            else return value;
        }
    }

    private double primary() {
        enter();
        try {
            if (eat('(')) {
                double v = expression();
                if (!eat(')')) throw new CalculationException(SYNTAX);
                return v;
            }
            if (eat('π')) return Math.PI;
            if (eat('√')) return squareRoot(parenthesizedArgument());
            skipSpaces();
            if (pos >= source.length()) throw new CalculationException(SYNTAX);
            char ch = source.charAt(pos);
            if (Character.isDigit(ch) || ch == '.') return number();
            if (Character.isLetter(ch)) {
                String name = identifier().toLowerCase(Locale.ROOT);
                if (name.equals("pi")) return Math.PI;
                if (name.equals("e")) return Math.E;
                if (name.equals("ans")) return finite(answer);
                double arg = parenthesizedArgument();
                return function(name, arg);
            }
            throw new CalculationException(SYNTAX);
        } finally { leave(); }
    }

    private double number() {
        skipSpaces();
        int start = pos;
        boolean digit = false;
        while (pos < source.length() && isAsciiDigit(source.charAt(pos))) {
            pos++; digit = true;
        }
        if (pos < source.length() && source.charAt(pos) == '.') {
            pos++;
            while (pos < source.length() && isAsciiDigit(source.charAt(pos))) {
                pos++; digit = true;
            }
        }
        if (!digit) throw new CalculationException(SYNTAX);
        // Display uses E for scientific notation, lowercase e is Euler's number.
        if (pos < source.length() && source.charAt(pos) == 'E') {
            int exponentStart = pos++;
            if (pos < source.length() && (source.charAt(pos) == '+' || source.charAt(pos) == '-')) pos++;
            int expDigits = pos;
            while (pos < source.length() && isAsciiDigit(source.charAt(pos))) pos++;
            if (expDigits == pos) pos = exponentStart;
        }
        try {
            return finite(Double.parseDouble(source.substring(start, pos)));
        } catch (NumberFormatException ex) {
            throw new CalculationException(SYNTAX);
        }
    }

    private static boolean isAsciiDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    private String identifier() {
        int start = pos;
        while (pos < source.length() && Character.isLetter(source.charAt(pos))) pos++;
        return source.substring(start, pos);
    }

    private double parenthesizedArgument() {
        if (!eat('(')) throw new CalculationException(SYNTAX);
        double value = expression();
        if (!eat(')')) throw new CalculationException(SYNTAX);
        return value;
    }

    private double function(String name, double x) {
        switch (name) {
            case "sin": return nearZero(Math.sin(angle(x)));
            case "cos": return nearZero(Math.cos(angle(x)));
            case "tan": {
                double angle = angle(x);
                if (Math.abs(Math.cos(angle)) < 1e-12) throw new CalculationException(DOMAIN);
                return nearZero(Math.tan(angle));
            }
            case "sqrt": return squareRoot(x);
            case "ln":
                if (x <= 0) throw new CalculationException(DOMAIN);
                return finite(Math.log(x));
            case "log":
                if (x <= 0) throw new CalculationException(DOMAIN);
                return finite(Math.log10(x));
            case "abs": return finite(Math.abs(x));
            default: throw new CalculationException(SYNTAX);
        }
    }

    private double angle(double x) {
        return mode == AngleMode.DEGREES ? Math.toRadians(x) : x;
    }

    private static double squareRoot(double x) {
        if (x < 0) throw new CalculationException(DOMAIN);
        return finite(Math.sqrt(x));
    }

    private static double factorial(double x) {
        if (x < 0 || x != Math.floor(x)) throw new CalculationException(DOMAIN);
        if (x > 170) throw new CalculationException(OVERFLOW);
        double result = 1;
        for (int i = 2; i <= (int) x; i++) result *= i;
        return finite(result);
    }

    private static double nearZero(double x) {
        return Math.abs(x) < 1e-12 ? 0.0 : finite(x);
    }

    private static double finite(double x) {
        if (Double.isNaN(x)) throw new CalculationException(DOMAIN);
        if (Double.isInfinite(x)) throw new CalculationException(OVERFLOW);
        return x;
    }

    private boolean eat(char expected) {
        skipSpaces();
        if (pos < source.length() && source.charAt(pos) == expected) {
            pos++;
            return true;
        }
        return false;
    }

    private void skipSpaces() {
        while (pos < source.length() && Character.isWhitespace(source.charAt(pos))) pos++;
    }

    private void enter() {
        if (++depth > MAX_DEPTH) throw new CalculationException(COMPLEXITY);
    }

    private void leave() { depth--; }
}
