package techtest.originalservice;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import techtest.originalservice.api.Bet;
import techtest.originalservice.api.BetsRequest;
import techtest.originalservice.api.BetsResponse;
import techtest.originalservice.api.OriginalService;

/**
 * An implementation of OriginalService which uses stub files.
 */
public class OriginalServiceStub implements OriginalService {
    private String stubFileFolder;

    private MappingJackson2HttpMessageConverter converter;

    public OriginalServiceStub(String stubFileFolder, MappingJackson2HttpMessageConverter converter) {
        super();

        Assert.isTrue(Files.exists(Paths.get(stubFileFolder)), "stubFileFolder must be a folder name that exists");
        Assert.notNull(converter, "converter");

        this.stubFileFolder = stubFileFolder;
        this.converter = converter;
    }

    @Override
    public BetsResponse bets(BetsRequest bet) {
        Path stubFile = Paths.get(stubFileFolder, "bets");
        techtest.originalservice.dto.BetsResponse betsResponse = readValue(stubFile,
                techtest.originalservice.dto.BetsResponse.class);
        return betsResponse.toApi();
    }

    @Override
    public Bet[] available() {
        Path stubFile = Paths.get(stubFileFolder, "available");
        techtest.originalservice.dto.Bet[] bets = readValue(stubFile, techtest.originalservice.dto.Bet[].class);
        return Arrays.stream(bets).map(techtest.originalservice.dto.Bet::toApi).toArray(Bet[]::new);
    }

    private <T> T readValue(Path path, Class<T> type) {
        try {
            File f = path.toFile().getAbsoluteFile();
            if (!f.isFile()) {
                // We're outputting the full filename in the error because this is stub mode.
                // You would not want to do this in a prod environment for security reasons
                // (leaking internal information).
                throw new IOException(f + " is not a file");
            }
            return converter.getObjectMapper().readValue(f, type);
        } catch (JsonParseException e) {
            throw new InternalException(e);
        } catch (JsonMappingException e) {
            throw new InternalException(e);
        } catch (IOException e) {
            throw new InternalException(e);
        }
    }

    public static class InternalException extends RuntimeException {
        public InternalException(Throwable cause) {
            super(cause);
        }
    }
}
