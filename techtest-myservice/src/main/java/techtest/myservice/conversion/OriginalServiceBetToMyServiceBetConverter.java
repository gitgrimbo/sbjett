package techtest.myservice.conversion;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

import techtest.myservice.api.Bet;

public class OriginalServiceBetToMyServiceBetConverter implements Converter<techtest.originalservice.api.Bet, Bet> {

    private FractionalOddsToDecimalOddsConverter fractionalOddsToDecimalOddsConverter;

    @Autowired
    public OriginalServiceBetToMyServiceBetConverter(
            FractionalOddsToDecimalOddsConverter fractionalOddsToDecimalOddsConverter) {
        super();
        this.fractionalOddsToDecimalOddsConverter = fractionalOddsToDecimalOddsConverter;
    }

    @Override
    public Bet convert(techtest.originalservice.api.Bet source) {
        BigDecimal bdOdds = fractionalOddsToDecimalOddsConverter.convert(source.getOdds());
        return new Bet(source.getBetId(), source.getEvent(), source.getName(), bdOdds);
    }

}
