package techtest.originalservice.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bets request DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BetsRequest {
    long bet_id;
    Odds odds;
    BigDecimal stake;

    public BetsRequest(techtest.originalservice.api.BetsRequest api) {
        this.bet_id = api.getBetId();
        this.odds = new Odds(api.getOdds());
        this.stake = api.getStake();
    }

    public techtest.originalservice.api.BetsRequest toApi() {
        return new techtest.originalservice.api.BetsRequest(bet_id, odds.toApi(), stake);
    }
}
