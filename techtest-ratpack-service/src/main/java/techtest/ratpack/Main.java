package techtest.ratpack;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ratpack.error.internal.DefaultDevelopmentErrorHandler;
import ratpack.exec.Promise;
import ratpack.handling.Context;
import ratpack.http.client.HttpClient;
import ratpack.server.RatpackServer;
import techtest.originalservice.api.OriginalService;

/**
 * This is a Ratpack application that implements the techtest API.
 * 
 * It demonstrates an async way of handling requests.
 * 
 * It uses the techtest.originalservice.api.OriginalService.BusinessLogicException class from the
 * techtest-original-service-api project, but no other shared classes.
 * 
 * The app is configured with the "techtest.remoteServiceUrl" system property.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        new Main().start();
    }

    String baseUrl;

    /**
     * Starts the application.
     * 
     * @throws Exception
     */
    public void start() throws Exception {
        // Config
        String baseUrlPropName = "techtest.remoteServiceUrl";
        baseUrl = System.getProperty(baseUrlPropName, "http://skybettechtestapi.herokuapp.com");

        // sanity check the URL
        if (!UrlValidator.getInstance().isValid(baseUrl)) {
            throw new IllegalArgumentException("System property " + baseUrlPropName + " must be a valid URL. Was: " + baseUrl);
        }

        RatpackServer.start(spec -> {
            spec.registryOf(rspec -> {
                // The DefaultDevelopmentErrorHandler outputs a lot more error info.
                rspec.add(new DefaultDevelopmentErrorHandler());
            }).handlers(chain -> {
                // Add the two handlers, using Java 8 method references.
                // If a request is made for a resource we don't handle, Ratpack will send a HTTP 404
                // with message "Client error 404".
                // If the HTTP method is invalid, Ratpack will send a HTTP 405 with message "Client
                // error 405".
                chain.get("available", this::handleGetAvailable);
                chain.post("bets", this::handlePostBets);
            });
        });
    }

    /**
     * Handler method for available bets.
     * 
     * Delegates to getAvailable() to get the bets from the remote service.
     * 
     * @param ctx
     * @throws URISyntaxException
     */
    private void handleGetAvailable(Context ctx) throws URISyntaxException {
        getAvailable(ctx).map(remoteAvailableBets -> {
            return remoteAvailableBets.stream().map(remoteBet -> {
                BigDecimal odds = decimalOddsFromFractionalOdds(remoteBet.getOdds());
                return new AvailableBet(remoteBet.getBet_id(), remoteBet.getEvent(), remoteBet.getName(), odds);
            }).collect(Collectors.toList());
        }).then(availableBets -> {
            sendJson(ctx, availableBets);
        });
    }

    /**
     * handler method for bets.
     * 
     * Delegates to postBets() to post the bet to the remote service.
     * 
     * @param ctx
     */
    private void handlePostBets(Context ctx) {
        ObjectMapper mapper = ctx.get(ObjectMapper.class);

        ctx.getRequest().getBody().map(reqbody -> {
            return mapper.readValue(reqbody.getInputStream(), BetsRequest.class);
        }).flatMap(betsRequest -> {
            return getAvailable(ctx).flatMap(remoteAvailableBets -> {
                RemoteOdds fractionalOdds = checkOdds(betsRequest.getBet_id(), betsRequest.getOdds(), remoteAvailableBets);
                RemoteBetsRequest remoteBetsRequest = new RemoteBetsRequest(betsRequest.getBet_id(), fractionalOdds,
                        betsRequest.getStake());
                return postBets(ctx, remoteBetsRequest);
            });
        }).onError(OriginalService.BusinessLogicException.class, e -> {
            sendJson(ctx, new DefaultError(e.getError().getDescription()));
        }).onError(e -> {
            // Any other error is treated as INVALID_BET
            sendJson(ctx, new DefaultError(OriginalService.Error.INVALID_BET_ID.getDescription()));
        }).then(remoteBetsResponse -> {
            sendJson(ctx, convertRemoteBetsResponse(remoteBetsResponse));
        });
    }

    /**
     * Uses the Ratpack HttpClient to GET the RemoteAvailableBet objects.
     * 
     * @param ctx
     * @return
     * @throws URISyntaxException
     */
    private Promise<List<RemoteAvailableBet>> getAvailable(Context ctx) throws URISyntaxException {
        HttpClient httpClient = ctx.get(HttpClient.class);
        ObjectMapper mapper = ctx.get(ObjectMapper.class);

        return httpClient.get(new URI(baseUrl + "/available")).map(remoteResponse -> {
            InputStream in = remoteResponse.getBody().getInputStream();
            return mapper.readValue(in, new TypeReference<List<RemoteAvailableBet>>() {
            });
        });
    }

    /**
     * Uses the Ratpack HttpClient to POST a bet.
     * 
     * @param ctx
     * @param remoteBetsRequest
     * @return
     * @throws URISyntaxException
     */
    private Promise<RemoteBetsResponse> postBets(Context ctx, RemoteBetsRequest remoteBetsRequest) throws URISyntaxException {
        HttpClient httpClient = ctx.get(HttpClient.class);
        ObjectMapper mapper = ctx.get(ObjectMapper.class);

        return httpClient.post(new URI(baseUrl + "/bets"), action -> {
            action.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
            action.body(respbody -> {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                mapper.writeValue(baos, remoteBetsRequest);
                respbody.bytes(baos.toByteArray());
            });
        }).map(remoteResponse -> {
            return mapper.readValue(remoteResponse.getBody().getText(), RemoteBetsResponse.class);
        });
    }

    private BetsResponse convertRemoteBetsResponse(RemoteBetsResponse remoteBetsResponse) {
        // Convert from RemoteBetsResponse to BetsResponse
        BigDecimal bdOdds = decimalOddsFromFractionalOdds(remoteBetsResponse.getOdds());
        return new BetsResponse(remoteBetsResponse.getBet_id(), remoteBetsResponse.getEvent(), remoteBetsResponse.getName(),
                bdOdds, remoteBetsResponse.getStake(), remoteBetsResponse.getTransaction_id());

    }

    /**
     * Lightweight version of MainController.checkOdds(). See that javadoc on that method for more
     * info.
     */
    private RemoteOdds checkOdds(long betId, BigDecimal submittedOdds, List<RemoteAvailableBet> bets) {
        Optional<RemoteAvailableBet> betWithId = findAvailableBet(betId, bets);

        // If the betWithId is found, convert its fractional odds to decimal.
        // Compare these decimal odds to those submitted by the user.
        // If they do not match, throw an INCORRECT_ODDS error.
        Optional<RemoteOdds> odds = betWithId.map(bet -> {
            BigDecimal bdOdds = decimalOddsFromFractionalOdds(bet.getOdds());

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
     *            The betId to look for.
     * @param bets
     *            The List of bets to look through.
     * @return
     */
    private Optional<RemoteAvailableBet> findAvailableBet(long betId, List<RemoteAvailableBet> bets) {
        // Using Java 8 Steams and Optional to align with techtest technical requirements.
        return bets.stream()
                // filter to find the matching bet
                .filter(bet -> bet.getBet_id() == betId)
                // return the first filtered item (if any)
                .findFirst();
    }

    private BigDecimal decimalOddsFromFractionalOdds(RemoteOdds odds) {
        return new BigDecimal(odds.getNumerator()).divide(new BigDecimal(odds.getDenominator())).add(BigDecimal.ONE);
    }

    /**
     * Serialises a JSON object to the response.
     * 
     * @param ctx
     * @param ob
     *            The object to serialise.
     */
    private static void sendJson(Context ctx, Object ob) {
        ObjectMapper mapper = ctx.get(ObjectMapper.class);
        ctx.getResponse().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        try {
            ctx.getResponse().send(mapper.writeValueAsBytes(ob));
        } catch (JsonProcessingException e) {
            ctx.getResponse().status(500);
            ctx.getResponse().send("Error serializing user to JSON");
        }
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
