package techtest.myservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import techtest.originalservice.OriginalServiceClient;
import techtest.originalservice.api.OriginalService;

@Profile("prod")
@Configuration
public class ConfigProd {
    // This URL value must be passed in as configuration if the "prod" profile is active.
    @Value("${techtest.remoteServiceUrl:http://skybettechtestapi.herokuapp.com}")
    private String remoteServiceUrl;

    @Bean
    public OriginalService originalServiceClient() {
        return new OriginalServiceClient(remoteServiceUrl, new RestTemplate());
    }
}
