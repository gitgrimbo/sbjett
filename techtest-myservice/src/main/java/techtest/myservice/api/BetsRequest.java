package techtest.myservice.api;

import java.math.BigDecimal;

import lombok.Value;

/**
 * Immutable bets request object.
 */
@Value
public class BetsRequest {
    long betId;
    BigDecimal odds;
    BigDecimal stake;
}
