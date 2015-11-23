package techtest.myservice.conversion;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;

import techtest.myservice.api.Bet;
import techtest.myservice.conversion.FractionalOddsToDecimalOddsConverter;
import techtest.myservice.conversion.OriginalServiceBetToMyServiceBetConverter;
import techtest.originalservice.api.Odds;

/**
 * In these tests, x_f_y means a fractional odds of x/y, and x_pt_y means a decimal odds of x.y.
 * 
 * For more comprehensive fractional/decimal and decimal/fractional tests, see the respective test
 * classes.
 */
public class OriginalServiceBetToMyServiceBetConverterTest {
    long betId = 1;
    String event = "event";
    String name = "name";

    OriginalServiceBetToMyServiceBetConverter converter;

    @Before
    public void before() {
        FractionalOddsToDecimalOddsConverter f2d = new FractionalOddsToDecimalOddsConverter();
        converter = new OriginalServiceBetToMyServiceBetConverter(f2d);
    }

    @Test(expected = NullPointerException.class)
    public void when_converting_null_then_throw_NPE() {
        converter.convert(null);
    }

    @Test
    public void when_converting_1_f_1_then_decimal_is_2() {
        assertConversion(1, 1, "2");
    }

    private void assertConversion(int numerator, int denominator, String decimalStr) {
        Bet actual = converter.convert(bet(new Odds(numerator, denominator)));
        assertThat(actual, is(mybet(new BigDecimal(decimalStr))));
    }

    private techtest.originalservice.api.Bet bet(Odds odds) {
        return new techtest.originalservice.api.Bet(betId, event, name, odds);
    }

    private Bet mybet(BigDecimal odds) {
        return new Bet(betId, event, name, odds);
    }
}
