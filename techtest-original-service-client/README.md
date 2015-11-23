# techtest-original-service-client - Background

This project contains a client for the `skybettechtestapi` web service.

It is an implementation of the `OriginalService` interface which is defined in the `techtest-original-service-api` API project.

It might be a bit heavyweight for the techtest, but in real life it might be useful to have a standalone strongly-typed client for the remote web service.

The footnotes at the end of this file (and the parts of this file that refer to them) are probably the most interesting thing about this README.

## Implementation

The client is implemented using Spring's [`RestTemplate`](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/client/RestTemplate.html).

It uses annotations from the [Project Lombok](https://projectlombok.org/index.html) project to create mutable data transfer objects (DTOs).

## Testing

The client is tested using Spring's [`MockRestServiceServer`](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/test/web/client/MockRestServiceServer.html).

See `techtest.originalservice.OriginalServiceClientTest`.

-----

# Remote Service Exploration

To create the client code, I have poked and prodded the remote service to see how it behaves with differing input.  This section explores that.

Base URL: http://skybettechtestapi.herokuapp.com.

## General

Invalid (non-existent) endpoints result in HTTP 404. E.g. "/available1".

    HTTP 404
    Cannot GET /available1

Using an invalid HTTP method with an endpoint. E.g. POST to "/available".

    HTTP 404
    Cannot POST /available

or GET to "/bets".

    HTTP 404
    Cannot GET /bets

## /available

Response is an array of available bet objects.

    [
      {
        "bet_id": 1,
        "event": "World Cup 2018",
        "name": "England",
        "odds": {
          "numerator": 10,
          "denominator": 1
        }
      },
      ...

### Errors

No unique errors for this endpoint?

## /bets

Request, e.g.:

    {
      "bet_id": 1,
      "odds": {
        "numerator": 10,
        "denominator": 1
      },
      "stake": 1
    }

Response is a bet confirmation object.

    {
      "bet_id": 1,
      "event": "World Cup 2018",
      "name": "England",
      "odds": {
        "numerator": 10,
        "denominator": 1
      },
      "stake": 10,
      "transaction_id": 810753
    }

### Errors

Missing or invalid "bet_id". E.g. `bet_id = 999`.

    HTTP 418
    {
      "error": "Invalid Bet ID"
    }

**NOTE** a string "bet_id" works if the numeric value of that string is a valid "bet_id". E.g. `bet_id = "1"`. See [1].

**NOTE** a string "bet_id" works if the value is `true`, as `true` can be coerced into `1`, and `1` is a valid "bet_id". See [1].

**NOTE** a string "bet_id" works if the value is `"0x1"`, as `"0x1"` can be coerced into `1`, and `1` is a valid "bet_id". See [1].

Zero or negative "bet_id".

    HTTP 500
    Internal Server Error

Modified odds.

    HTTP 418
    {
      "error": "Incorrect Odds"
    }

Missing odds.

    HTTP 418
    {
      "error": "Invalid Odds"
    }

Invalid or missing "stake".  E.g. `stake = -1`.

    HTTP 418
    {
      "error": "Invalid Stake"
    }

**NOTE** a string "stake" works if the numeric value of that string is a valid "stake". E.g. `stake = "10"` is ok. `stake = "e"` is invalid. See [1].

**NOTE** a "stake" works to several decimal places. E.g. `stake = 0.123456789`. See [1].

**NOTE** `stake = "0xA"` is ok (`10` in hex). See [1].

**NOTE** `stake = []` is ok (probably evaluates to `0`). See [1].

**NOTE** `stake = true` is ok (probably evaluates to `1`). `stake = false` is ok (probably evaluates to `0`). See [1].

**NOTE** `stake = {}` is invalid. See [1].

**NOTE** `stake = null` is valid (probably evaluates to `0`). See [1].

**NOTE** `stake = undefined` is invalid. See [1].

# Footnotes

**[1] Type coercion -** If the server is Node.js (or Nashorn, i.e. running a JavaScript runtime), then these anomalies are probably due to JavaScript's type coercion. So if a field ought to be a number, and the submitted value can be coerced by the JS runtime into a number that is valid for that field, then the value is valid.  E.g. from Chrome's console, if `+value` results in a number, then the value can be coerced into a number and could be 'valid':

    +[]
    0
    +{}
    NaN
    +"1"
    1
    +true
    1
    +false
    0
    +null
    0
    +undefined
    NaN
    +"0xa"
    10
