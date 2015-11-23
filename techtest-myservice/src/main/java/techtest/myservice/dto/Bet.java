package techtest.myservice.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bet DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bet {
    long bet_id;
    String event;
    String name;
    BigDecimal odds;

    public Bet(techtest.myservice.api.Bet apiBet) {
        this.bet_id = apiBet.getBetId();
        this.event = apiBet.getEvent();
        this.name = apiBet.getName();
        this.odds = apiBet.getOdds();
    }
}
