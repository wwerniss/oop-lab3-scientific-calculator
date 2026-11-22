package ua.edu.oop.scicalc.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class ResultFormatterTest {
    @Test public void handlesIntegersAndDecimals() {
        assertEquals("2", ResultFormatter.format(2));
        assertEquals("2.5", ResultFormatter.format(2.5));
        assertEquals("0", ResultFormatter.format(-0.0));
        assertEquals("0", ResultFormatter.format(1e-15));
        assertEquals("-2.5", ResultFormatter.format(-2.5));
    }
    @Test public void usesExponentForVeryLargeOrSmallValues() {
        assertTrue(ResultFormatter.format(1.2e20).contains("E"));
        assertTrue(ResultFormatter.format(1.5e-10).contains("E"));
        assertEquals("100000000000", ResultFormatter.format(1e11));
    }
    @Test public void rejectsInfinity() {
        try { ResultFormatter.format(Double.POSITIVE_INFINITY); fail(); }
        catch (CalculationException ex) { assertEquals(CalculationException.Type.OVERFLOW, ex.getType()); }
    }
}
