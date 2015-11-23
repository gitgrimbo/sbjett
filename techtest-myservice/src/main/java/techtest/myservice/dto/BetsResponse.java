package techtest.myservice.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bets response DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BetsResponse {
    long bet_id;
    String event;
    String name;
    BigDecimal odds;
    BigDecimal stake;
    long transaction_id;

    public BetsResponse(techtest.myservice.api.BetsResponse apiResponse) {
        this.bet_id = apiResponse.getBetId();
        this.event = apiResponse.getEvent();
        this.name = apiResponse.getName();
        this.odds = apiResponse.getOdds();
        this.stake = apiResponse.getStake();
        this.transaction_id = apiResponse.getTransactionId();
    }
}
