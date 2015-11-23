package techtest.myservice.dto;

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
    BigDecimal odds;
    BigDecimal stake;

    public techtest.myservice.api.BetsRequest toApi() {
        return new techtest.myservice.api.BetsRequest(bet_id, odds, stake);
    }
}
