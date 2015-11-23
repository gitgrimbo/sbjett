# techtest-myservice - Background

This project contains an implementation of a Spring Boot application that uses Spring MVC to expose a web service API.

It has two primary [`Controllers`](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/mvc.html), `MainController` and `LightweightController`.

`MainController` uses the `techtest-original-service-client` project as its way of interacting with the remote service.

`LightweightController` is an all-in-one-class solution.

## Running

### MainController

Execute the JAR and activate the `prod` profile.  Optionally pass a `techtest.remoteServiceUrl` system property to the remote service.  This is the base URL of the remote service to proxy/adapt.  This defaults to `http://skybettechtestapi.herokuapp.com` if not provided.

````
java -Dspring.profiles.active=prod
     -Dtechtest.remoteServiceUrl=http://skybettechtestapi.herokuapp.com
     -jar target\techtest-myservice-0.0.1-SNAPSHOT.jar
````

### LightweightController

Same as above, but activate the `lightweight` profile in addition to `prod`.

````
java -Dspring.profiles.active=prod,lightweight
     -jar target\techtest-myservice-0.0.1-SNAPSHOT.jar
````

## Testing

The `MainController` is tested using Spring's [`MockMvc`](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/test/web/servlet/MockMvc.html).

See `techtest.myservice.MainControllerTest`.

# Bet conversion

Fractional to decimal is straightforward.  But how to convert back?  Given I haven't come up with a general fool-proof way to convert arbitrary decimal odds to their fractional version I instead retrieve the available bets again before every POST to `"/bets"`. Then I:
- find the matching bet by id
- get the fractional odds for that bet
- convert this fractional odds to decimal
- and then compare the decimal with the decimal odds the user wants to submit
