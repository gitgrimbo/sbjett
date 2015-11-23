package techtest.myservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import techtest.myservice.conversion.DecimalOddsToFractionalOddsConverter;
import techtest.myservice.conversion.FractionalOddsToDecimalOddsConverter;
import techtest.myservice.conversion.OriginalServiceBetToMyServiceBetConverter;

/**
 * Collection of converter beans exposed in a @Configuration class.
 */
@Configuration
public class ConverterConfig {
    @Bean
    public FractionalOddsToDecimalOddsConverter fractionalOddsToDecimalOddsConverter() {
        return new FractionalOddsToDecimalOddsConverter();
    }

    @Bean
    public DecimalOddsToFractionalOddsConverter decimalOddsToFractionalOddsConverter() {
        return new DecimalOddsToFractionalOddsConverter();
    }

    @Bean
    public OriginalServiceBetToMyServiceBetConverter originalServiceBetToMyServiceBetConverter() {
        return new OriginalServiceBetToMyServiceBetConverter(fractionalOddsToDecimalOddsConverter());
    }
}
