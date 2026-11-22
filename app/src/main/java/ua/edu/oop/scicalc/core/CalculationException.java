package ua.edu.oop.scicalc.core;

/** A machine-readable error to be localized by the Android UI. */
public final class CalculationException extends RuntimeException {
    public enum Type { SYNTAX, DIVISION_BY_ZERO, DOMAIN, OVERFLOW, COMPLEXITY }

    private final Type type;

    public CalculationException(Type type) {
        super(type.name());
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
