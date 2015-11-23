package techtest.originalservice;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import techtest.originalservice.api.Bet;
import techtest.originalservice.api.BetsRequest;
import techtest.originalservice.api.BetsResponse;
import techtest.originalservice.api.Odds;
import techtest.originalservice.api.OriginalService;
import techtest.originalservice.api.OriginalService.BusinessLogicException;
import techtest.originalservice.api.OriginalService.InternalErrorException;

/**
 * These tests test the OriginalServiceClient using MockRestServiceServer.
 * 
 * The MockRestServiceServer can be used to create hand-crafted responses for RestTemplate, and we
 * use these responses to test the behaviour of OriginalServiceClient.
 */
public class OriginalServiceClientTest {
    // Some fixture data
    long betId = 1;
    Odds odds = new Odds(10, 1);

    String resourceRoot = "/techtest/originalservice/api/";
    String baseUrl = "http://skybettechtestapi.herokuapp.com";
    OriginalService originalService;
    RestTemplate restTemplate;
    MockRestServiceServer mockServer;

    @Before
    public void before() {
        restTemplate = new RestTemplate();
        originalService = new OriginalServiceClient(baseUrl, restTemplate);
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void there_are_6_bets_available() throws IOException {
        mockServer.expect(relativeRequestTo("/available"))
                .andRespond(withSuccess(testData("available"), MediaType.APPLICATION_JSON_UTF8));

        Bet[] actual = originalService.available();

        assertThat(actual, notNullValue());
        assertThat(actual.length, is(6));
    }

    @Test(expected = NullPointerException.class)
    public void when_bets_with_null_request_then_NPE() {
        originalService.bets(null);
    }

    @Test
    public void when_valid_bet_made_then_valid_response_returned() throws IOException {
        mockServer.expect(relativeRequestTo("/bets"))
                .andRespond(withStatus(HttpStatus.CREATED).body(testData("bets")).contentType(MediaType.APPLICATION_JSON_UTF8));

        BetsRequest req = betsRequest();
        BetsResponse actual = originalService.bets(req);

        // assert that the response matches the test data.
        assertThat(actual, notNullValue());
        assertThat(actual.getBetId(), is(1L));
        assertThat(actual.getStake(), is(new BigDecimal("10")));
        assertThat(actual.getOdds(), is(new Odds(10, 1)));
    }

    @Test
    public void when_bet_made_with_invalid_betid_then_business_logic_exception_thrown_with_invalid_bet_id_error()
            throws IOException {
        mockServer.expect(relativeRequestTo("/bets")).andRespond(withStatus(HttpStatus.I_AM_A_TEAPOT)
                .body(testData("bets_invalidOrMissingBetId")).contentType(MediaType.APPLICATION_JSON_UTF8));

        // This invalid bet id isn't really necessary, as the mockServer will return a fixed
        // response.
        long invalidBetId = 9999;
        BetsRequest req = betsRequestWithId(invalidBetId);
        try {
            originalService.bets(req);
        } catch (BusinessLogicException e) {
            assertThat(e.getError(), is(OriginalService.Error.INVALID_BET_ID));
        }
    }

    /**
     * The remote service returns a HTTP 500 and a plain String message ("Internal Server Error\n")
     * when the "bet_id" is zero or negative.
     * 
     * We test that this is propagated as an InternalErrorException.
     */
    @Test
    public void when_bet_made_with_zero_bet_id_then_internal_error_exception_thrown() throws IOException {
        mockServer.expect(relativeRequestTo("/bets")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(testData("bets_zeroOrNegativeBetId")).contentType(MediaType.APPLICATION_JSON_UTF8));

        long invalidBetId = 0;
        BetsRequest req = betsRequestWithId(invalidBetId);
        try {
            originalService.bets(req);
            fail("Expected exception");
        } catch (InternalErrorException e) {
            // ok
        }
    }

    /**
     * The remote service returns a HTTP 418 and a JSON error when the "stake" is invalid. See the
     * README.md for a breakdown on what is valid and invalid.
     * 
     * We test that this is propagated as a BusinessLogicException.
     */
    @Test
    public void when_bet_made_with_invalid_stake_then_business_logic_exception_thrown_with_invalid_stake_error()
            throws IOException {
        mockServer.expect(relativeRequestTo("/bets")).andRespond(withStatus(HttpStatus.I_AM_A_TEAPOT)
                .body(testData("bets_invalidStake")).contentType(MediaType.APPLICATION_JSON_UTF8));

        BigDecimal invalidStake = new BigDecimal("-1");
        BetsRequest req = betsRequestWithStake(invalidStake);
        try {
            originalService.bets(req);
        } catch (BusinessLogicException e) {
            assertThat(e.getError(), is(OriginalService.Error.INVALID_STAKE));
        }
    }

    /**
     * The remote service returns a HTTP 418 and a JSON error when the "odds" are incorrect (are
     * different to what is on the remote server).
     * 
     * We test that this is propagated as a BusinessLogicException.
     */
    @Test
    public void when_bet_made_with_incorrect_odds_then_business_logic_exception_thrown_with_incorrect_odds_error()
            throws IOException {
        mockServer.expect(relativeRequestTo("/bets")).andRespond(withStatus(HttpStatus.I_AM_A_TEAPOT)
                .body(testData("bets_incorrectOdds")).contentType(MediaType.APPLICATION_JSON_UTF8));

        Odds incorrectOdds = new Odds(999, 1);
        BetsRequest req = betsRequestWithOdds(incorrectOdds);
        try {
            originalService.bets(req);
        } catch (BusinessLogicException e) {
            assertThat(e.getError(), is(OriginalService.Error.INCORRECT_ODDS));
        }
    }

    /**
     * The remote service returns a HTTP 418 and a JSON error when the "odds" are incorrect (are
     * different to what is on the remote server).
     * 
     * We test that this is propagated as a BusinessLogicException.
     */
    @Test
    public void when_bet_made_with_invalid_odds_then_business_logic_exception_thrown_with_invalid_odds_error()
            throws IOException {
        mockServer.expect(relativeRequestTo("/bets")).andRespond(withStatus(HttpStatus.I_AM_A_TEAPOT)
                .body(testData("bets_invalidOdds")).contentType(MediaType.APPLICATION_JSON_UTF8));

        // The request doesn't really matter here as the mockServer response is fixed.
        // We can't really create an invalid odds object here anyway, because our service is
        // strongly-typed.
        BetsRequest req = betsRequest();
        try {
            originalService.bets(req);
        } catch (BusinessLogicException e) {
            assertThat(e.getError(), is(OriginalService.Error.INVALID_ODDS));
        }
    }

    BetsRequest betsRequestWithId(long betId) {
        return new BetsRequest(betId, odds, BigDecimal.ONE);
    }

    BetsRequest betsRequestWithStake(BigDecimal stake) {
        return new BetsRequest(betId, odds, BigDecimal.ONE);
    }

    BetsRequest betsRequestWithOdds(Odds odds) {
        return new BetsRequest(betId, odds, BigDecimal.ONE);
    }

    BetsRequest betsRequest() {
        return new BetsRequest(betId, odds, BigDecimal.ONE);
    }

    RequestMatcher relativeRequestTo(String expectedUri) {
        // the expected URL is always absolute
        return requestTo(baseUrl + expectedUri);
    }

    String testData(String resource) throws IOException {
        InputStream in = getClass().getResourceAsStream(resourceRoot + "/" + resource);
        return StreamUtils.copyToString(in, Charset.forName("UTF8"));
    }
}
