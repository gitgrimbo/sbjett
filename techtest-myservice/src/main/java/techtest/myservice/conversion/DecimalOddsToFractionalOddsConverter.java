package techtest.myservice.conversion;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;

import techtest.originalservice.api.Odds;

public class DecimalOddsToFractionalOddsConverter implements Converter<BigDecimal, Odds> {

    Map<BigDecimal, Odds> mappings = new HashMap<BigDecimal, Odds>() {
        {
            put(new BigDecimal("11.0"), new Odds(10, 1));
            put(new BigDecimal("2.0"), new Odds(1, 1));
            put(new BigDecimal("4.0"), new Odds(3, 1));
            put(new BigDecimal("2.75"), new Odds(7, 4));
            put(new BigDecimal("3.0"), new Odds(2, 1));
            put(new BigDecimal("18.0"), new Odds(17, 1));
        }
    };

    @Override
    public Odds convert(BigDecimal source) {
        return mappings.get(source);
    }

}
