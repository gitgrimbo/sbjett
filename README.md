# Start Here

This folder contains a multi-module `pom.xml` which will build all the projects needed.

See the `README.md` files in each project folder for more information.

There are basically three attempts at the web service:

1. `MainController` in `techtest-myservice`. This is a traditional Spring Boot MVC `@Controller`, that delegates access to the remote service (http://skybettechtestapi.herokuapp.com) to a strongly-typed client implemented using Spring `RestTemplate` (the client lives in `techtest-original-service-client`).
2. `LightweightController` in `techtest-myservice`. This is a Spring Boot MVC `@Controller` that does not use the strongly-typed client.
3. `Main` in `techtest-ratpack-service`.  This is an implementation of the service using [Ratpack](https://ratpack.io/).

The `techtest-original-service-api` and `techtest-original-service-client` projects are supporting projects for `techtest-myservice`.

## Build

Run `mvn clean package`.

Hopefully you will see something like this afterwards.  This step might take a long time the first time when Maven downloads the dependencies.

````
[INFO] techtest-original-service-api ...................... SUCCESS [  2.659 s]
[INFO] techtest-original-service-client ................... SUCCESS [  3.366 s]
[INFO] techtest-myservice ................................. SUCCESS [  8.158 s]
[INFO] techtest-ratpack-service ........................... SUCCESS [  1.508 s]
[INFO] techtest ........................................... SUCCESS [  0.004 s]
````

And see the two fat-jars, once for `techtest-myservice` and one for `techtest-ratpack-service`.

````
dir *.jar /s | findstr jar
22/11/2015  14:56        17,423,294 techtest-myservice-0.0.1-SNAPSHOT.jar
22/11/2015  14:55            10,484 techtest-original-service-api-1.0.0.jar
22/11/2015  14:55            16,464 techtest-original-service-client-0.0.1-SNAPSHOT.jar
22/11/2015  14:56        26,199,500 techtest-ratpack-service-0.0.1-SNAPSHOT.jar
````

See the `README.md` in each project for more details including how to run the fat-jars.

For info, I used these versions:

````
Apache Maven 3.3.9 (bb52d8502b132ec0a5a3f4c09453c07478323dc5; 2015-11-10T16:41:47+00:00)
Maven home: c:\java\maven\3.3.9
Java version: 1.8.0_65, vendor: Oracle Corporation
Java home: c:\java\jdk\1.8.0_65\jre
Default locale: en_GB, platform encoding: Cp1252
OS name: "windows 7", version: "6.1", arch: "amd64", family: "dos"
````
