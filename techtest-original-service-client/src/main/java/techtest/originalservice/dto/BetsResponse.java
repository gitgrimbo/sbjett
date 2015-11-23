package techtest.originalservice.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bets response object DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BetsResponse {
    String error;
    long bet_id;
    String event;
    String name;
    Odds odds;
    BigDecimal stake;
    long transaction_id;

    public techtest.originalservice.api.BetsResponse toApi() {
        return new techtest.originalservice.api.BetsResponse(bet_id, event, name, odds.toApi(), stake, transaction_id);
    }
}
