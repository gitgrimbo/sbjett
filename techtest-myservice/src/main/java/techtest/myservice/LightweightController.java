package techtest.myservice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import techtest.originalservice.api.OriginalService;

/**
 * This is an alternative, lightweight (and lightly-typed) controller responsible for handling
 * incoming requests.
 * 
 * This controller does not go through an intermediate service layer, and instead is more
 * 'script-like' and simply 'does what it needs to'.
 */
@RestController
@Profile("lightweight")
public class LightweightController {
    RestTemplate restTemplate = new RestTemplate();

    String root = "http://skybettechtestapi.herokuapp.com";

    @RequestMapping(value = "/available", method = {RequestMethod.GET})
    public ResponseEntity<List<AvailableBet>> available() {
        ResponseEntity<List<RemoteAvailableBet>> original = availableInternal();

        List<AvailableBet> converted = original.getBody().stream().map(remoteBet -> {
            BigDecimal odds = decimalOddsFromFractionalOdds(remoteBet.getOdds());
            return new AvailableBet(remoteBet.getBet_id(), remoteBet.getEvent(), remoteBet.getName(), odds);
        }).collect(Collectors.toList());

        return new ResponseEntity<>(converted, removeContentLength(original.getHeaders()), original.getStatusCode());
    }

    /**
     * Catch-all for any other non-GET requests for "/available".
     * 
     * The remote service returns a String of the form "Cannot $METHOD $PATH\n", so do we the same.
     */
    @RequestMapping(value = "/available")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String available_anyOtherMethod(HttpMethod method, HttpServletRequest request) {
        return "Cannot " + method.name() + " " + request.getRequestURI() + "\n";
    }

    @RequestMapping(value = "/bets", method = {RequestMethod.POST}, consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<BetsResponse> bets(@RequestBody BetsRequest betsRequest) {
        String url = root + "/bets";

        // Convert BetsRequest to RemoteBetsRequest
        RemoteOdds fractionalOdds = checkOdds(betsRequest);
        RemoteBetsRequest remoteBetsRequest = new RemoteBetsRequest(betsRequest.getBet_id(), fractionalOdds,
                betsRequest.getStake());

        // Make remote call
        ResponseEntity<RemoteBetsResponse> remoteBetsEntity = restTemplate.postForEntity(url, remoteBetsRequest,
                RemoteBetsResponse.class);
        RemoteBetsResponse remoteBetsResponse = remoteBetsEntity.getBody();

        // Convert RemoteBetsResponse to BetsResponse
        BigDecimal bdOdds = decimalOddsFromFractionalOdds(remoteBetsResponse.getOdds());
        BetsResponse betsResponse = new BetsResponse(remoteBetsResponse.getBet_id(), remoteBetsResponse.getEvent(),
                remoteBetsResponse.getName(), bdOdds, remoteBetsResponse.getStake(), remoteBetsResponse.getTransaction_id());

        return new ResponseEntity<>(betsResponse, removeContentLength(remoteBetsEntity.getHeaders()),
                remoteBetsEntity.getStatusCode());
    }

    /**
     * Catch-all for any other non-POST, non-application/json requests for "/bets".
     * 
     * The remote service returns a String of the form "Cannot $METHOD $PATH\n", so do we the same.
     */
    @RequestMapping(value = "/bets")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String bets_anyOtherMethod(HttpMethod method, HttpServletRequest request) {
        return "Cannot " + method.name() + " " + request.getRequestURI() + "\n";
    }

    private ResponseEntity<List<RemoteAvailableBet>> availableInternal() {
        String url = root + "/available";

        // Preserve typing with ParameterizedTypeReference
        // http://stackoverflow.com/a/13820584
        return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<RemoteAvailableBet>>() {
        });
    }

    /**
     * Lightweight version of MainController.checkOdds(). See that javadoc on that method for more
     * info.
     */
    private RemoteOdds checkOdds(BetsRequest betsRequest) {
        Optional<RemoteAvailableBet> betWithId = findAvailableBet(betsRequest.getBet_id());

        // If the betWithId is found, convert its fractional odds to decimal.
        // Compare these decimal odds to those submitted by the user.
        // If they do not match, throw an INCORRECT_ODDS error.
        Optional<RemoteOdds> odds = betWithId.map(bet -> {
            BigDecimal bdOdds = decimalOddsFromFractionalOdds(bet.getOdds());
            BigDecimal submittedOdds = betsRequest.getOdds();

            // compareTo() is safer than equals() as the BigDecimals may have have different scales.
            // https://docs.oracle.com/javase/8/docs/api/java/math/BigDecimal.html#compareTo-java.math.BigDecimal-
            if (0 != bdOdds.compareTo(submittedOdds)) {
                throw new OriginalService.BusinessLogicException(OriginalService.Error.INCORRECT_ODDS);
            }

            return bet.getOdds();
        });

        // If we have odds (which implies we found a matching bet), then return them.
        // Else throw an INVALID_BET_ID error.
        return odds.orElseThrow(() -> new OriginalService.BusinessLogicException(OriginalService.Error.INVALID_BET_ID));
    }

    /**
     * @param betId
     * @return
     */
    private Optional<RemoteAvailableBet> findAvailableBet(long betId) {
        // Make request to original service
        ResponseEntity<List<RemoteAvailableBet>> available = availableInternal();

        // Using Java 8 Steams and Optional to align with techtest technical requirements.
        return available.getBody().stream()
                // filter to find the matching bet
                .filter(bet -> bet.getBet_id() == betId)
                // return the first filtered item (if any)
                .findFirst();
    }

    private BigDecimal decimalOddsFromFractionalOdds(RemoteOdds odds) {
        return new BigDecimal(odds.getNumerator()).divide(new BigDecimal(odds.getDenominator())).add(BigDecimal.ONE);
    }

    /**
     * Removes the "Content-Length" header from the remote response. Changing the odds means the
     * "Content-Length" will no longer be correct.
     * 
     * @param headers
     * @return the modified headers.
     */
    private MultiValueMap<String, String> removeContentLength(HttpHeaders headers) {
        HttpHeaders modified = new HttpHeaders();
        modified.putAll(headers);
        modified.remove(HttpHeaders.CONTENT_LENGTH);
        return modified;
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
