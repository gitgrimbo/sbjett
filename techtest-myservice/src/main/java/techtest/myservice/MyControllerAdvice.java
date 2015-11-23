package techtest.myservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import techtest.myservice.MainController.DefaultError;
import techtest.originalservice.api.OriginalService.BusinessLogicException;

/**
 * Controller advice to perform general exception handling.
 */
@ControllerAdvice(basePackageClasses = {MainController.class, LightweightController.class})
public class MyControllerAdvice extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MyControllerAdvice.class);

    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    DefaultError handleControllerException(Exception e) {
        LOG.error(e.getMessage(), e);

        return new DefaultError("Internal Error");
    }

    /**
     * This ExceptionHandler handles all HttpStatusCodeException thrown by the RestTemplate. I.e.,
     * all client and server errors. See org.springframework.web.client.DefaultResponseErrorHandler.
     * 
     * We return the response along as-is, rather than letting Spring's more general error handling
     * serialize out the Exception.
     * 
     * @param e
     * @return
     */
    @ExceptionHandler(HttpStatusCodeException.class)
    @ResponseBody
    ResponseEntity<Object> handleHttpStatusCodeException(HttpStatusCodeException e) {
        LOG.error(e.getMessage(), e);

        return new ResponseEntity<>(e.getResponseBodyAsByteArray(), e.getResponseHeaders(), e.getStatusCode());
    }

    /**
     * @param e
     * @return
     */
    @ExceptionHandler(BusinessLogicException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.I_AM_A_TEAPOT)
    DefaultError handleBusinessLogicException(BusinessLogicException e) {
        LOG.error(e.getMessage(), e);

        return new DefaultError(e.getError().getDescription());
    }
}
