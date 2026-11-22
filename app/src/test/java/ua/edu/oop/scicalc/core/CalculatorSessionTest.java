package ua.edu.oop.scicalc.core;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class CalculatorSessionTest {
    @Test public void defaultState() {
        CalculatorSession s = new CalculatorSession();
        assertEquals("", s.getExpression());
        assertEquals("0", s.getResult());
        assertEquals(AngleMode.DEGREES, s.getAngleMode());
    }

    @Test public void keypadAndEquals() {
        CalculatorSession s = new CalculatorSession();
        for (String key : new String[]{"2", "+", "3", "×", "4", "="}) s.press(key);
        assertEquals("14", s.getResult());
        assertEquals(14, s.getLastAnswer(), 0);
        assertEquals(1, s.getHistory().size());
        assertEquals("2+3×4 = 14", s.getHistory().get(0).asLine());
    }

    @Test public void continuesFromResultAndResetsOnNumber() {
        CalculatorSession s = new CalculatorSession();
        s.press("9"); s.press("="); s.press("+"); s.press("1"); s.press("=");
        assertEquals("10", s.getResult());
        s.press("5");
        assertEquals("5", s.getExpression());
    }

    @Test public void usesAns() {
        CalculatorSession s = new CalculatorSession();
        s.press("6"); s.press("="); s.press("Ans"); s.press("×"); s.press("2"); s.press("=");
        assertEquals("12", s.getResult());
    }

    @Test public void functionsAndDeleteAsUnit() {
        CalculatorSession s = new CalculatorSession();
        s.press("sin");
        assertEquals("sin(", s.getExpression());
        s.press("DEL");
        assertEquals("", s.getExpression());
        s.press("√"); s.press("9"); s.press(")"); s.press("=");
        assertEquals("3", s.getResult());
    }

    @Test public void degreeAndRadianSwitch() {
        CalculatorSession s = new CalculatorSession();
        s.press("RAD");
        assertEquals(AngleMode.RADIANS, s.getAngleMode());
        s.press("DEG");
        assertEquals(AngleMode.DEGREES, s.getAngleMode());
    }

    @Test public void translatesErrorWithoutCrashing() {
        CalculatorSession s = new CalculatorSession();
        s.press("1"); s.press("÷"); s.press("0"); s.press("=");
        assertEquals(CalculationException.Type.DIVISION_BY_ZERO, s.getError());
        assertEquals(0, s.getHistory().size());
        s.press("DEL");
        assertNull(s.getError());
    }

    @Test public void clearKeepsAnswerAndHistory() {
        CalculatorSession s = new CalculatorSession();
        s.press("7"); s.press("="); s.press("AC");
        assertEquals("", s.getExpression());
        assertEquals(7, s.getLastAnswer(), 0);
        assertEquals(1, s.getHistory().size());
    }

    @Test public void historyIsCappedAndReadOnly() {
        CalculatorSession s = new CalculatorSession();
        for (int i = 0; i < 20; i++) {
            s.press("AC"); s.press("2"); s.press("=");
        }
        assertEquals(CalculatorSession.MAX_HISTORY, s.getHistory().size());
        try { s.getHistory().clear(); fail("Expected immutable view"); }
        catch (UnsupportedOperationException expected) { /* expected */ }
        s.clearHistory();
        assertTrue(s.getHistory().isEmpty());
    }

    @Test public void restoreSessionAndHistory() {
        CalculatorSession s = new CalculatorSession();
        s.restore("sin(90)", "1", 9, AngleMode.RADIANS, true);
        assertEquals(9, s.getLastAnswer(), 0);
        assertEquals(AngleMode.RADIANS, s.getAngleMode());
        s.restoreHistory(List.of(new HistoryEntry("1+1", "2")));
        assertEquals("1+1 = 2", s.getHistory().get(0).asLine());
    }

    @Test public void previewDoesNotThrowOnIncompleteExpression() {
        CalculatorSession s = new CalculatorSession();
        s.press("("); s.press("2"); s.press("+");
        assertEquals("…", s.getResult());
        assertNull(s.getError());
    }

    @Test public void convenienceButtons() {
        CalculatorSession s = new CalculatorSession();
        s.press("5"); s.press("x²"); s.press("=");
        assertEquals("25", s.getResult());
        s.press("1/x"); s.press("=");
        assertEquals("0.04", s.getResult());
    }
}
