package techtest.myservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import techtest.originalservice.OriginalServiceStub;
import techtest.originalservice.api.OriginalService;

@Profile("stub")
@Configuration
public class ConfigStub {
    // This folder should be passed in as configuration if the "stub" profile is active, but it
    // will default to a location that is known to have stub files.
    @Value("${techtest.stubFileFolder:src/test/resources/techtest/originalservice/api}")
    private String stubFileFolder;

    @Autowired
    MappingJackson2HttpMessageConverter httpMessageConverter;

    @Bean
    public OriginalService originalServiceStub() {
        return new OriginalServiceStub(stubFileFolder, httpMessageConverter);
    }
}
