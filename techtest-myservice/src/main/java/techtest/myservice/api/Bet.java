package techtest.myservice.api;

import java.math.BigDecimal;

import lombok.Value;

/**
 * Immutable bet object.
 */
@Value
public class Bet {
    long betId;
    String event;
    String name;
    BigDecimal odds;
}
