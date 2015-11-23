package techtest.originalservice.api;

import java.math.BigDecimal;

import lombok.Value;

/**
 * Immutable bets request object.
 */
@Value
public class BetsRequest {
    long betId;
    Odds odds;
    BigDecimal stake;
}
