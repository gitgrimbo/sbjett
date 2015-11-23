package techtest.originalservice.api;

import java.math.BigDecimal;

import lombok.Value;

/**
 * Immutable bets response object.
 */
@Value
public class BetsResponse {
    long betId;
    String event;
    String name;
    Odds odds;
    BigDecimal stake;
    long transactionId;
}
