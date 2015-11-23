# techtest-ratpack-service - Background

This project contains an implementation of a Ratpack application to expose a web service API.


## Running

Execute the JAR and optionally pass a `techtest.remoteServiceUrl` system property to the remote service.  This is the base URL of the remote service to proxy/adapt.  This defaults to `http://skybettechtestapi.herokuapp.com` if not provided.

````
java -Dtechtest.remoteServiceUrl=http://skybettechtestapi.herokuapp.com
     -jar target\techtest-ratpack-service-0.0.1-SNAPSHOT.jar
````

## Testing

Unfortunately there are no tests for the Ratpack service.
