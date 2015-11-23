package techtest.originalservice;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriTemplateHandler;

import techtest.originalservice.api.Bet;
import techtest.originalservice.api.BetsRequest;
import techtest.originalservice.api.BetsResponse;
import techtest.originalservice.api.OriginalService;

/**
 * An implementation of OriginalService which uses a Spring RestTemplate to connect to the remote
 * endpoint.
 * 
 * This might be an unnecessary abstraction for the techtest, but such a client may be useful in
 * scenarios other than proxying the original service.
 */
public class OriginalServiceClient implements OriginalService {
    private String baseUrl;
    private RestTemplate restTemplate;

    public OriginalServiceClient(String baseUrl, RestTemplate restTemplate) {
        super();

        Assert.hasText(baseUrl, "baseUrl must be a valid url");
        Assert.notNull(restTemplate, "restTemplate");

        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
        ((DefaultUriTemplateHandler) restTemplate.getUriTemplateHandler()).setBaseUrl(baseUrl);
        restTemplate.setErrorHandler(new MyResponseErrorHandler());
    }

    @Override
    public BetsResponse bets(BetsRequest betsRequest) {
        // Map dto to api
        techtest.originalservice.dto.BetsRequest reqDto = new techtest.originalservice.dto.BetsRequest(betsRequest);

        // Call the remote service
        ResponseEntity<techtest.originalservice.dto.BetsResponse> respEntity = this.restTemplate.postForEntity("/bets", reqDto,
                techtest.originalservice.dto.BetsResponse.class);

        return respEntity.getBody().toApi();
    }

    @Override
    public Bet[] available() {
        techtest.originalservice.dto.Bet[] bets = this.restTemplate.getForObject("/available",
                techtest.originalservice.dto.Bet[].class);

        // Get stream of response to be used to convert to correct return type
        return Arrays.stream(bets)
                // Map dto bets to api bets
                .map(techtest.originalservice.dto.Bet::toApi)
                // terminal
                .toArray(Bet[]::new);
    }

    /**
     * Responsible for mapping any errors that come out of RestTemplate.
     * 
     * Some responses from the remote service as HTTP 500s with a simple text message. This
     * ResponseErrorHandler caters for those as well as the standard JSON errors that the remote web
     * service uses.
     */
    class MyResponseErrorHandler extends DefaultResponseErrorHandler {
        /**
         * Quick and dirty map of error messages to 'business logic' exceptions. This would be be
         * externalised somewhere.
         */
        Map<String, OriginalService.Error> errorMap = new HashMap<String, OriginalService.Error>() {
            {
                put("Invalid Odds", Error.INVALID_ODDS);
                put("Incorrect Odds", Error.INCORRECT_ODDS);
                put("Invalid Bet ID", Error.INVALID_BET_ID);
                put("Invalid Stake", Error.INVALID_STAKE);
            }
        };

        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            if (response.getStatusCode().is5xxServerError()) {
                // The HTTP 500 errors from the server are NOT JSON. So any attempt to process as
                // such will fail. Therefore we abort early here.
                throw new InternalErrorException(
                        "Would normally create an alert that something could be wrong with the remote service");
            }

            ResponseExtractor<techtest.originalservice.dto.Error> extractor = new HttpMessageConverterExtractor(
                    techtest.originalservice.dto.Error.class, restTemplate.getMessageConverters());

            techtest.originalservice.dto.Error errorDto = extractor.extractData(response);

            OriginalService.Error error = errorMap.get(errorDto.getError());

            if (null == error) {
                error = OriginalService.Error.UNKNOWN;
            }

            throw new BusinessLogicException(error);
        }
    }
}
