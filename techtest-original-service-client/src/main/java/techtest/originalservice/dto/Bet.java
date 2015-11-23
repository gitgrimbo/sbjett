package techtest.originalservice.dto;

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
    Odds odds;

    public techtest.originalservice.api.Bet toApi() {
        return new techtest.originalservice.api.Bet(bet_id, event, name, odds.toApi());
    }
}
