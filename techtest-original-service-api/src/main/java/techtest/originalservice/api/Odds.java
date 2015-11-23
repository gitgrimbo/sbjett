package techtest.originalservice.api;

import lombok.Value;

/**
 * Immutable fractional odds object.
 */
@Value
public class Odds {
    int numerator;
    int denominator;

    public Odds(int numerator, int denominator) {
        super();
        if (numerator < 1) {
            throw new IllegalArgumentException("numerator cannot be less than one: " + denominator);
        }
        if (denominator < 1) {
            throw new IllegalArgumentException(
                    "denominator cannot be less than one: " + denominator);
        }
        this.numerator = numerator;
        this.denominator = denominator;
    }
}
