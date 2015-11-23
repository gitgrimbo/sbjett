package techtest.myservice;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import techtest.myservice.conversion.FractionalOddsToDecimalOddsConverter;
import techtest.myservice.conversion.OriginalServiceBetToMyServiceBetConverter;
import techtest.myservice.dto.Bet;
import techtest.myservice.dto.BetsRequest;
import techtest.myservice.dto.BetsResponse;
import techtest.originalservice.api.Odds;
import techtest.originalservice.api.OriginalService;
import techtest.originalservice.api.OriginalService.BusinessLogicException;

/**
 * This is the main controller responsible for handling incoming requests.
 * 
 * It will perform the conversions to and from the data types used in the original
 * "http://skybettechtestapi.herokuapp.com/" service.
 * 
 * It uses an implementation of the OriginalService interface to access the remote service.
 * 
 * This extra static typing and abstraction could be considered overkill for the techtest, but it
 * does mean we have a standalone Java client of the original service available to us if we need it.
 * 
 * POSSIBLE IMPROVEMENTS:
 * 
 * This controller's methods are synchronous (as is the OriginalService Java interface). It could be
 * more performant to use async requests - https://spring.io/guides/gs/async-method/.
 */
@RestController
// Only activate this Controller when the lightweight profile (which has the LightweightController)
// is not active.
@Profile("!lightweight")
public class MainController {
    @Autowired
    OriginalService originalService;

    @Autowired
    OriginalServiceBetToMyServiceBetConverter inwardBetConverter;

    @Autowired
    FractionalOddsToDecimalOddsConverter f2dOddsConverter;

    @RequestMapping(value = "/available", method = {RequestMethod.GET})
    public Bet[] available() {
        // Make request to original service
        techtest.originalservice.api.Bet[] origResponse = originalService.available();

        // Using Java 8 Streams to align with techtest technical requirements.
        // Get stream of response to be used to convert to correct return type
        return Arrays.stream(origResponse)
                // Map original service api to myservice api
                .map(inwardBetConverter::convert)
                // Map myservice api to myservice dto
                .map(Bet::new)
                // terminal. collect the the myservice dto objects into an array.
                .toArray(Bet[]::new);
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
    @ResponseStatus(HttpStatus.CREATED)
    public BetsResponse bets(@RequestBody BetsRequest request) {
        // Map myservice dto to myservice api
        techtest.myservice.api.BetsRequest myApiRequest = request.toApi();

        Odds odds = checkOdds(myApiRequest);

        // Map myservice api to original service api
        techtest.originalservice.api.BetsRequest origApiReq = new techtest.originalservice.api.BetsRequest(
                myApiRequest.getBetId(), odds, myApiRequest.getStake());

        // Make request to original service
        techtest.originalservice.api.BetsResponse origApiResp = originalService.bets(origApiReq);

        // Map original service api to myservice api
        BigDecimal bdOdds = f2dOddsConverter.convert(origApiResp.getOdds());
        techtest.myservice.api.BetsResponse myApiResponse = new techtest.myservice.api.BetsResponse(origApiResp.getBetId(),
                origApiResp.getEvent(), origApiResp.getName(), bdOdds, origApiResp.getStake(), origApiResp.getTransactionId());

        // Map myservice api to myservice dto
        return new BetsResponse(myApiResponse);
    }

    /**
     * We add POST RequestMapping with no "consumes" value to handle those POST requests that don't
     * use the JSON content type.
     * 
     * Spring would otherwise return a 415 Unsupported Media Type, but the techtest server responds
     * with TEAPOT and a JSON error object.
     */
    @RequestMapping(value = "/bets", method = {RequestMethod.POST})
    @ResponseStatus(HttpStatus.I_AM_A_TEAPOT)
    public DefaultError bets_notJson() {
        return new DefaultError("Invalid Bet ID");
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

    /**
     * Not quite sure of another way to do this correctly. Without a foolproof way of mapping an
     * arbitrary decimal back to its original fraction, I am instead getting the latest fractional
     * odds from the remote server, finding the odds for the corresponding bet, converting this to
     * decimal, and comparing the decimals.
     * 
     * If there was a strict limited set of valid decimal odds, then we could externalise a table
     * with the mappings from these to fractional odds, and avoid having to contact the remote
     * server.
     * 
     * The trade off here is the correctness of the data versus the extra expense of calling the
     * remote service.
     * 
     * @param betsRequest
     *            The request to check the odds for.
     * 
     * @return The fractional odds for the betsRequest.
     * 
     * @throws BusinessLogicException
     *             (error=OriginalService.Error.INCORRECT_ODDS). if the odds don't match.
     */
    private Odds checkOdds(techtest.myservice.api.BetsRequest betsRequest) {
        Optional<techtest.originalservice.api.Bet> betWithId = findAvailableBet(betsRequest.getBetId());

        // If the betWithId is found, convert its fractional odds to decimal.
        // Compare these decimal odds to those submitted by the user.
        // If they do not match, throw an INCORRECT_ODDS error.
        Optional<Odds> odds = betWithId.map(bet -> {
            BigDecimal bdOdds = f2dOddsConverter.convert(bet.getOdds());
            BigDecimal submittedOdds = betsRequest.getOdds();

            // compareTo() is safer than equals() as the BigDecimals may have have different scales.
            // https://docs.oracle.com/javase/8/docs/api/java/math/BigDecimal.html#compareTo-java.math.BigDecimal-
            if (0 != bdOdds.compareTo(submittedOdds)) {
                throw new BusinessLogicException(OriginalService.Error.INCORRECT_ODDS);
            }

            return bet.getOdds();
        });

        // If we have odds (which implies we found a matching bet), then return them.
        // Else throw an INVALID_BET_ID error.
        return odds.orElseThrow(() -> new BusinessLogicException(OriginalService.Error.INVALID_BET_ID));
    }

    /**
     * Attempts to find an available bet with matching betId.
     * 
     * @param betId
     * @return
     */
    private Optional<techtest.originalservice.api.Bet> findAvailableBet(long betId) {
        // Make request to original service
        techtest.originalservice.api.Bet[] available = originalService.available();

        // Using Java 8 Steams and Optional to align with techtest technical requirements.
        return Arrays.stream(available)
                // filter to find the matching bet
                .filter(bet -> bet.getBetId() == betId)
                // return the first filtered item (if any)
                .findFirst();
    }

    /**
     * Our default error response.
     */
    public static class DefaultError {
        public String error = "Unknown";

        public DefaultError() {
        }

        public DefaultError(String error) {
            super();
            this.error = error;
        }
    }
}
