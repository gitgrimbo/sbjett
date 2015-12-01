package controllers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import play.Logger;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WS;
import play.mvc.Controller;
import play.mvc.Result;
import techtest.originalservice.api.OriginalService;

public class Application extends Controller {
    String baseUrl = "http://skybettechtestapi.herokuapp.com";

    /**
     * TODO.
     *
     * @return Promise<Result>
     */
    public Promise<Result> available() {
        return remoteAvailable().map(available -> {
            JsonNode destJson = Json.toJson(mapAvailable(available));
            return ok(destJson).as("application/json");
        });
    }

    private Promise<List<RemoteAvailableBet>> remoteAvailable() {
        String url = baseUrl + "/available";
        return WS.url(url).get().map(r -> Json.mapper().readValue(r.getBody(), new TypeReference<List<RemoteAvailableBet>>() {
        }));
    }

    /**
     * TODO.
     *
     * @return Promise<Result>
     */
    public Promise<Result> bets() {
        String url = baseUrl + "/bets";

        BetsRequest betsRequest = Json.fromJson(request().body().asJson(), BetsRequest.class);

        return checkOdds(betsRequest).flatMap(remoteOdds -> {

            RemoteBetsRequest remoteBetsRequest = mapBetsRequest(betsRequest, remoteOdds);

            Promise<Result> result = WS.url(url).setContentType("application/json").post(Json.toJson(remoteBetsRequest))
                    //
                    .map(r -> Json.mapper().readValue(r.getBody(), RemoteBetsResponse.class))
                    //
                    .map(this::mapRemoteBetsResponse)
                    //
                    .map(betsResponse -> ok(Json.toJson(betsResponse)).as("application/json"));

            return result.recoverWith(th -> {
                Logger.info("3");
                Logger.info("" + th);
                return Promise.pure(status(500, th.getMessage()).as("text/plain"));
            });
        });
    }

    /**
     * Returns a Promise of RemoteOdds for the bet that matches the bet_id in betsRequest.
     */
    private Promise<RemoteOdds> checkOdds(BetsRequest betsRequest) {
        return findAvailableBet(betsRequest.getBet_id()).map(betWithId -> {

            // If the betWithId is found, convert its fractional odds to decimal.
            // Compare these decimal odds to those submitted by the user.
            // If they do not match, throw an INCORRECT_ODDS error.
            Optional<RemoteOdds> odds = betWithId.map(bet -> {
                BigDecimal bdOdds = decimalOddsFromFractionalOdds(bet.getOdds());
                BigDecimal submittedOdds = betsRequest.getOdds();

                // compareTo() is safer than equals() as the BigDecimals may have have different
                // scales.
                // https://docs.oracle.com/javase/8/docs/api/java/math/BigDecimal.html#compareTo-java.math.BigDecimal-
                if (0 != bdOdds.compareTo(submittedOdds)) {
                    throw new OriginalService.BusinessLogicException(OriginalService.Error.INCORRECT_ODDS);
                }

                return bet.getOdds();
            });

            // If we have odds (which implies we found a matching bet), then return them.
            // Else throw an INVALID_BET_ID error.
            return odds.orElseThrow(() -> new OriginalService.BusinessLogicException(OriginalService.Error.INVALID_BET_ID));
        });
    }

    /**
     * @param betId
     * @return
     */
    private Promise<Optional<RemoteAvailableBet>> findAvailableBet(long betId) {
        // Make request to original service
        return remoteAvailable().map(available -> {
            // Using Java 8 Steams and Optional to align with techtest technical requirements.
            return available.stream()
                    // filter to find the matching bet
                    .filter(bet -> bet.getBet_id() == betId)
                    // return the first filtered item (if any)
                    .findFirst();
        });
    }

    private BigDecimal decimalOddsFromFractionalOdds(RemoteOdds odds) {
        return new BigDecimal(odds.getNumerator()).divide(new BigDecimal(odds.getDenominator())).add(BigDecimal.ONE);
    }

    // ----------
    // DTO handling
    // ----------

    private RemoteBetsRequest mapBetsRequest(BetsRequest betsRequest, RemoteOdds odds) {
        return new RemoteBetsRequest(betsRequest.getBet_id(), odds, betsRequest.getStake());
    }

    private BetsResponse mapRemoteBetsResponse(RemoteBetsResponse remoteBetsResponse) {
        BigDecimal bdOdds = decimalOddsFromFractionalOdds(remoteBetsResponse.getOdds());
        return new BetsResponse(remoteBetsResponse.getBet_id(), remoteBetsResponse.getEvent(), remoteBetsResponse.getName(),
                bdOdds, remoteBetsResponse.getStake(), remoteBetsResponse.getTransaction_id());
    }

    private List<AvailableBet> mapAvailable(List<RemoteAvailableBet> available) {
        return available.stream().map(remoteBet -> {
            BigDecimal odds = decimalOddsFromFractionalOdds(remoteBet.getOdds());
            return new AvailableBet(remoteBet.getBet_id(), remoteBet.getEvent(), remoteBet.getName(), odds);
        }).collect(Collectors.toList());
    }

    // ----------
    // Data Transfer Objects
    // ----------

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailableBet {
        long bet_id;
        String event;
        String name;
        BigDecimal odds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BetsRequest {
        long bet_id;
        BigDecimal odds;
        BigDecimal stake;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BetsResponse {
        long bet_id;
        String event;
        String name;
        BigDecimal odds;
        BigDecimal stake;
        long transaction_id;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RemoteOdds {
        int numerator;
        int denominator;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RemoteAvailableBet {
        long bet_id;
        String event;
        String name;
        RemoteOdds odds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RemoteBetsRequest {
        long bet_id;
        RemoteOdds odds;
        BigDecimal stake;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RemoteBetsResponse {
        long bet_id;
        String event;
        String name;
        RemoteOdds odds;
        BigDecimal stake;
        long transaction_id;
    }
}
