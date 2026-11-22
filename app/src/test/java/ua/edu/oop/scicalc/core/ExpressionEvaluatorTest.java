package ua.edu.oop.scicalc.core;

import org.junit.Test;
import static org.junit.Assert.*;
import static ua.edu.oop.scicalc.core.CalculationException.Type.*;

public class ExpressionEvaluatorTest {
    private double deg(String e) { return ExpressionEvaluator.evaluate(e, AngleMode.DEGREES, 0); }
    private double rad(String e) { return ExpressionEvaluator.evaluate(e, AngleMode.RADIANS, 0); }
    private void assertError(String e, CalculationException.Type expected) {
        try {
            deg(e);
            fail("Expected " + expected + " for " + e);
        } catch (CalculationException ex) {
            assertEquals(expected, ex.getType());
        }
    }

    @Test public void basicArithmeticAndOrder() {
        assertEquals(14, deg("2+3×4"), 0);
        assertEquals(20, deg("(2+3)*4"), 0);
        assertEquals(10, deg("20÷2"), 0);
        assertEquals(-7, deg("3−10"), 0);
        assertEquals(6, deg("2 + 4"), 0);
    }

    @Test public void nestedBracketsAndNegatives() {
        assertEquals(-4, deg("-2^2"), 0);
        assertEquals(4, deg("(-2)^2"), 0);
        assertEquals(0.125, deg("2^-3"), 1e-12);
        assertEquals(-10, deg("−(5+5)"), 0);
        assertEquals(36, deg("((2+4))^2"), 0);
    }

    @Test public void powerIsRightAssociative() {
        assertEquals(512, deg("2^3^2"), 0);
        assertEquals(81, deg("3^4"), 0);
        assertEquals(1, deg("5^0"), 0);
    }

    @Test public void factorialPercentAndInverse() {
        assertEquals(120, deg("5!"), 0);
        assertEquals(1, deg("0!"), 0);
        assertEquals(0.25, deg("25%"), 0);
        assertEquals(0.2, deg("5^(-1)"), 1e-12);
        assertEquals(14400, deg("5!^2"), 0);
    }

    @Test public void trigonometryDegrees() {
        assertEquals(0.5, deg("sin(30)"), 1e-12);
        assertEquals(0, deg("cos(90)"), 1e-12);
        assertEquals(1, deg("tan(45)"), 1e-12);
        assertEquals(0, deg("sin(180)"), 1e-12);
    }

    @Test public void trigonometryRadians() {
        assertEquals(1, rad("sin(π/2)"), 1e-12);
        assertEquals(1, rad("cos(0)"), 0);
        assertEquals(1, rad("tan(π/4)"), 1e-12);
        assertEquals(Math.PI, rad("pi"), 0);
    }

    @Test public void scientificFunctions() {
        assertEquals(4, deg("√(16)"), 0);
        assertEquals(5, deg("sqrt(25)"), 0);
        assertEquals(2, deg("log(100)"), 0);
        assertEquals(1, deg("ln(e)"), 1e-12);
        assertEquals(6, deg("abs(-6)"), 0);
    }

    @Test public void constantsAndImplicitMultiplication() {
        assertEquals(Math.PI * 2, deg("2π"), 1e-12);
        assertEquals(14, deg("2(3+4)"), 0);
        assertEquals(1.5, deg("3sin(30)"), 1e-12);
        assertEquals(5 * Math.E, deg("5e"), 1e-12);
        assertEquals(42, ExpressionEvaluator.evaluate("Ans+2", AngleMode.DEGREES, 40), 0);
    }

    @Test public void exponentialNumberFormat() {
        assertEquals(0.00000012, deg("1.2E-7"), 1e-16);
        assertEquals(300, deg("3E2"), 0);
    }

    @Test public void invalidSyntax() {
        assertError("", SYNTAX);
        assertError("2+", SYNTAX);
        assertError("sin 30", SYNTAX);
        assertError("(2+3", SYNTAX);
        assertError("2+3)", SYNTAX);
        assertError("banana(2)", SYNTAX);
        assertError(".", SYNTAX);
    }

    @Test public void illegalDomains() {
        assertError("√(-1)", DOMAIN);
        assertError("ln(0)", DOMAIN);
        assertError("log(-10)", DOMAIN);
        assertError("2.5!", DOMAIN);
        assertError("(-1)!", DOMAIN);
        assertError("tan(90)", DOMAIN);
    }

    @Test public void divisionAndOverflow() {
        assertError("2/0", DIVISION_BY_ZERO);
        assertError("1/(3-3)", DIVISION_BY_ZERO);
        assertError("171!", OVERFLOW);
        assertError("1E308*10", OVERFLOW);
    }

    @Test public void rejectsExcessivelyComplexInput() {
        StringBuilder deep = new StringBuilder();
        for (int i = 0; i < 90; i++) deep.append('(');
        deep.append('1');
        for (int i = 0; i < 90; i++) deep.append(')');
        assertError(deep.toString(), COMPLEXITY);
        assertError("1".repeat(301), COMPLEXITY);
    }
}
