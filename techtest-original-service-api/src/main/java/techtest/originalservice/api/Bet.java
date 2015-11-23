package techtest.originalservice.api;

import lombok.Value;

/**
 * Immutable bet object.
 */
@Value
public class Bet {
    long betId;
    String event;
    String name;
    Odds odds;
}
