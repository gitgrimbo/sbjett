package techtest.myservice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import techtest.myservice.MainControllerTest.Config;
import techtest.myservice.dto.BetsRequest;
import techtest.originalservice.OriginalServiceClient;
import techtest.originalservice.api.OriginalService;

/**
 * Tests the MainController. If the MainController is set up with an OriginalService impl that uses
 * the live site (http://skybettechtestapi.herokuapp.com) then it could be considered an integration
 * test, and would probably be run by Maven failsafe rather than as a unit test (surefire).
 * 
 * The MainController *is* set up as live by default, and so by default these tests act like
 * integration tests.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {Application.class, Config.class})
@WebAppConfiguration
public class MainControllerTest {
    @Autowired
    private WebApplicationContext wac;

    @Autowired
    MappingJackson2HttpMessageConverter httpMessageConverter;

    private MockMvc mockMvc;
    private BigDecimal bet1Odds = new BigDecimal("11.0");

    @Before
    public void before() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    public void when_get_available_then_ok() throws Exception {
        this.mockMvc.perform(get("/available")).andExpect(status().isOk());
    }

    @Test
    public void when_post_bets_with_no_request_body_then_teapot() throws Exception {
        this.mockMvc.perform(post("/bets")).andExpect(status().isIAmATeapot());
    }

    @Test
    public void when_post_bets_with_valid_request_body_then_created() throws Exception {
        BetsRequest betsRequest = new BetsRequest(1, bet1Odds, new BigDecimal(1));
        String content = httpMessageConverter.getObjectMapper().writeValueAsString(betsRequest);
        ResultActions resultActions = this.mockMvc
                .perform(post("/bets").content(content).contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        resultActions.andExpect(status().isCreated());
    }

    @Test
    public void when_post_bets_with_incorrect_odds_then_incorrect_odds_error() throws Exception {
        BigDecimal incorrectOdds = new BigDecimal("1.0");
        BetsRequest betsRequest = new BetsRequest(1, incorrectOdds, new BigDecimal(1));
        String content = httpMessageConverter.getObjectMapper().writeValueAsString(betsRequest);
        ResultActions resultActions = this.mockMvc
                .perform(post("/bets").content(content).contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        resultActions.andExpect(status().isIAmATeapot()).andExpect(content().string(jsonError("Incorrect Odds")));
    }

    @Test
    public void when_post_bets_with_invalid_stake_odds_then_invalid_stake_error() throws Exception {
        BigDecimal invalidStake = new BigDecimal(-1);
        BetsRequest betsRequest = new BetsRequest(1, bet1Odds, invalidStake);
        String content = httpMessageConverter.getObjectMapper().writeValueAsString(betsRequest);
        ResultActions resultActions = this.mockMvc
                .perform(post("/bets").content(content).contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        resultActions.andExpect(status().isIAmATeapot()).andExpect(content().string(jsonError("Invalid Stake")));
    }

    @Test
    public void when_post_available_http_404_and_cannot_get_message() throws Exception {
        this.mockMvc.perform(post("/available")).andExpect(status().isNotFound())
                .andExpect(content().string("Cannot POST /available\n"));
    }

    @Test
    public void when_get_bets_then_http_404_and_cannot_get_message() throws Exception {
        this.mockMvc.perform(get("/bets")).andExpect(status().isNotFound()).andExpect(content().string("Cannot GET /bets\n"));
    }

    String jsonError(String message) {
        return "{\"error\":\"" + message + "\"}";
    }

    @Configuration
    public static class Config {
        @Bean
        public OriginalService originalService() {
            return new OriginalServiceClient("http://skybettechtestapi.herokuapp.com", new RestTemplate());
        }
    }
}
