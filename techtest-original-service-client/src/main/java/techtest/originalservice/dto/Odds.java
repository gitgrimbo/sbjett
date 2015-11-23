package techtest.originalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fractional odds DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Odds {
    int numerator;
    int denominator;

    public Odds(techtest.originalservice.api.Odds odds) {
        this.numerator = odds.getNumerator();
        this.denominator = odds.getDenominator();
    }

    public techtest.originalservice.api.Odds toApi() {
        return new techtest.originalservice.api.Odds(numerator, denominator);
    }
}
