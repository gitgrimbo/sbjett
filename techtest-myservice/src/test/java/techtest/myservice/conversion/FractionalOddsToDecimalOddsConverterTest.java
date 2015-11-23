package techtest.myservice.conversion;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;

import techtest.originalservice.api.Odds;

/**
 * In these tests, x_f_y means a fractional odds of x/y, and x_pt_y means a decimal odds of x.y.
 * 
 * You could write this alternatively as a JUnit Parameterized test.
 */
public class FractionalOddsToDecimalOddsConverterTest {
    FractionalOddsToDecimalOddsConverter converter;

    @Before
    public void before() {
        converter = new FractionalOddsToDecimalOddsConverter();
    }

    @Test(expected = NullPointerException.class)
    public void when_converting_null_then_throw_NPE() {
        converter.convert(null);
    }

    @Test
    public void when_converting_1_f_1_then_decimal_is_2() {
        assertConversion(1, 1, "2");
    }

    @Test
    public void when_converting_2_f_1_then_decimal_is_3() {
        assertConversion(2, 1, "3");
    }

    @Test
    public void when_converting_1_f_2_then_decimal_is_1_pt_5() {
        assertConversion(1, 2, "1.5");
    }

    private void assertConversion(int numerator, int denominator, String decimalStr) {
        BigDecimal d = converter.convert(new Odds(numerator, denominator));
        assertThat(d, is(new BigDecimal(decimalStr)));
    }
}
