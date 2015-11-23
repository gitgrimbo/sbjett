package techtest.myservice.conversion;

import java.math.BigDecimal;

import org.springframework.core.convert.converter.Converter;

import techtest.originalservice.api.Odds;

/**
 * Convert from fractional odds to BigDecimal.
 */
public class FractionalOddsToDecimalOddsConverter implements Converter<Odds, BigDecimal> {

    @Override
    public BigDecimal convert(Odds source) {
        return new BigDecimal(source.getNumerator()).divide(new BigDecimal(source.getDenominator())).add(BigDecimal.ONE);
    }

}
