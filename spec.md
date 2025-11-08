# Node Babashka Ring Adapter

## Overview

Create a library that provides a ring-like interface built on-top of a
production grade node HTTP server.

### Goals

1. Provide a production ready http server
2. Provide a ring-like adapter on top of the http server
3. The Ring-like adapter should support custom node or express servers
4. Support rapid development leveraging ClojureScript's
5. Provide some middleware for common operations

## Language and Runtime

This library will be implemented in ClojureScript running on top of
node-babashka (nbb). This decision makes it more trivial to share resources
between a ClojureScript client and server for example reusing reagent hiccup
views for server-side rendering and client-side hydration.

## Server Implementation

The underlying http server should be built on top of Node 24+'s http library.
While requiring some wheel reinventing compared to libraries like Hono JS and
Express, it mitigates bigger performance hits converting request data to express
then to ring then back to express and back to the node Response primitive. This
reduces layers to Node's HTTP.Request -> ring -> HTTP.Response.

### Requests

The server feature leverages node's http library to create an http server that
transforms the http.IncomingMessage to a [ring-compatible req hash-map](https://github.com/ring-clojure/ring/blob/fd08dd8d905bc8062866cfec938c8cbf65afc7b0/ring-servlet/src/ring/util/servlet.clj#L44C26-L44C46).

#### Server Host Details

The server-port and server-name should be derived from the Host header. It
should match the address the request came from. If localhost is proxied, these
values should reflect the proxy.

If the port is not within the Host header, default to 80.

#### Remote Address

The remote-addr should reflect the IP string of the client. If a request is coming from
a proxy, then it should use the `X-Forwarded-For` header.

#### URI and URL

The URI should refer to the original request pathname of the request URL. This
can be used for routing with a simple cond or more structured routing middleware
later.

#### Query-string and Query

The query-string field should contain the raw search string identical to `new
URL(window.location).search`. The query field, while not included in the
official ring implementation should contain a hash-map of parsed query values
similar to `Object.fromEntries(new URL(window.location).searchParams.entries())`

#### Scheme, Request Method, and Protocol

The scheme refers to the protocol and http version if applicable such as
`HTTP/1.1`.

Request method should be parsed as a lower-case keyword like `:post`, `:put`, or
`:delete`.

The protocol can be read from the socket data or `x-forwarded-proto` when
available. It should reflect the protocol used to make the request, if behind a
https proxy then the protocol should be https.

#### Headers

Node's http.IncomingMessage object parses the headers as a plain object which
may be transformed into a hash-map using the builtin `js->clj` ClojureScript
function. Most headers use a comma separated list when dealing with multiple
values however the cookie header uses `;` to separate multiple cookies.

#### Content-Type and Content-Length

String values parsed by the `Content-Type` and `Content-Length` headers.
Content-Length is `nil` when the request does not contain the header.

#### Character Encoding

Parses the charset of the `Content-Type` header meta attribute. Defaults to
`nil` otherwise it's a `string`.

#### SSL Client Cert

The `ssl-client-cert` field is not likely useful as it's recommended to put the
node app behind another routing layer such as `nginx` or `caddy`. When a client
cert is available in the `request.socket` data, it is parsed into an
`X509Certificate` instance.

#### Body

Contains the request body as a node `stream.Readable` paired with an
AbortController if the socket connection closes early.

### Responses

The following fields in a res(ponse) hash-map will be applied to the
node http.ServerResponse object:

- status {number} - Numeric status such as 200, 404, 500, etc..., defaults to
  200
- headers {hash-map} - Mapping of header key-value pairs to send first. For
  example: `{:Content-Type "application/json"}`
- body {string} - Body data to send with the response after the headers. Does
  not stringify or serialize data by default, requires middleware.

### Middleware

This library should provide a few built-in middleware options to make some
common operations simple and easy.

```clojure
(p/-> default-handler
      (my-router)
      (my-middleware)
      (mw/json))
```

The middleware can be dropped into a composed middleware stack using the
promesa/-> thread operator to support async request handling without any
implementation complexity of endusers.

The `mw/json` middleware receives the req first before the stack and the res at
the end of the request cycle. This makes it trivial to support parsing JSON data
from the post body before any other middleware receives the request, and
serializing the body on response objects before sending to the browser after all
other middleware.

As an over-simplified example:

```clojure
(defn json
  [next-mw]
  (fn [req]
     (let [req (if (:body req)
                 (assoc req :body (js/JSON.parse (:body req)))
                 req)
           res (next-mw req)]
       (if (:body res)
         (assoc res :body (js/JSON.serialize (:body res)))
         res))))
```

#### default-handler

Middleware for returning a 404 by default for unhandled requests.

#### url-encoded-form-data

Middleware for parsing urlencoded form data. Commonly used for built-in browser
form submission processing.

#### json

Middleware for parsing incoming JSON body data from POST, PUT, PATCH, or DELETE
requests. Additionally handles serializing body data into a JSON string.

#### logging

Middleware for logging incoming request data and outgoing response metadata
including the time between processing a request and the response.

#### timeout

Middleware for handling request timeouts.

### Environment Variables

- PORT - number - The TCP socket port to listen on. Defaults to 3000 unless
  specified, if already in use and no PORT env var is set the library should find
  the next available port in a sequence until no EADDRINUSE error is thrown.
- ADDRESS - string - The network interface address to listen on. Defaults to
  `127.0.0.1` for security best practices.
- MAX_CORES - number - Optional env var to limit the max number of CPU cores the
  http server processes are forked to.
- TIMEOUT - number - Optional base request timeout. Defaults to 30 seconds
  `30000` milliseconds.
- KEEP_ALIVE_TIMEOUT - number - Optional timeout for how long the server should
  wait for additional incoming data after writing the last response, before a
  socket will be destroyed. [See Node
  Docs](https://nodejs.org/docs/latest/api/http.html#serverkeepalivetimeout)
- KEEP_ALIVE_TIMEOUT_BUFFER - number - Additional time added to the
  KEEP_ALIVE_TIMEOUT env var to mitigate ECONNRESET errors by extending the
  timeout window slightly. Defaults to 1000 (1 second). [See Node
  Docs](https://nodejs.org/docs/latest/api/http.html#serverkeepalivetimeoutbuffer)

#### Address

Customize the address the network interface the server listens on. It should
default to 127.0.0.1 to avoid introducing security risks accepting connections

#### Port

### Clustering

To maximize performance, cluster the servers to all available CPU cores by
default. This should leverage node's built-in clustering features with an env
var that can manually specify the max number of cores for fine tuning.

### Graceful Shutdown

When interrupting or terminating the node server process it should gracefully
shutdown all the subprocess servers and disconnect any connected sockets. This
should prevent unexpected behavior like restarting the server and it throwing
EADDRINUSE errors when no server is running.

### Headers and Timeouts

By default requests should have a 30 second timeout plus the buffer window
timeout before a socket is disconnected. The function for creating a http server
should set the timeout properties using an option value, or an env var, or a
default value if neither is present. This supports setting up multiple servers
if desired.

## Ring Adapter

### Ring Interface

#### Request

Request data should be transformed into a hash-map typically referred to as the
`req`. To match with ring it should have the following properties.

| name               | type            | Description                                                                                                                                                |
| ------------------ | --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| server-port        | number          | Port the server is listening on                                                                                                                            |
| server-name        | string          | The host name from the Host header of the incoming request                                                                                                 |
| remote-addr        | string          | IP of the client making the request. Should use the x-forwarded-for address if proxied                                                                     |
| uri                | string          | Location pathname of the request                                                                                                                           |
| query-string       | string          | Raw query string included in the request url like `page=2`                                                                                                 |
| scheme             | keyword         | Keyword of request protocol such as `:http` or `:https`                                                                                                    |
| request-method     | keyword         | Lowercase request method such as `:get`, `:post`, `:put`, `:patch`, or `:delete`                                                                           |
| protocol           | string          | Protocol + http version such as `HTTP/1.1`                                                                                                                 |
| headers            | hash-map        | Hash-map of keyword header names to values such as `{:content-type "text/html"}`                                                                           |
| content-type       | string          | Content-Type header, defaults to `nil`                                                                                                                     |
| content-length     | number          | Content-Length header, defaults to `0`                                                                                                                     |
| character-encoding | string          | Parsed from the charset of the `Content-Type` header                                                                                                       |
| ssl-client-cert    | X509Certificate | Parsed from `https` client socket in `https` connections. Defaults to `nil`                                                                                |
| body               | stream.Readable | Request body stream. It's wrapped around the original req but managed with an AbortController().signal for canceling requests when the socket disconnects. |

#### Response

Response data should be returned by ring middleware as a hash-map containing any
of the following properties:

| name    | type     | Default | Description                                                                                                                                       |
| ------- | -------- | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| status  | number   | 200     | HTTP Status code to return                                                                                                                        |
| headers | hash-map | {}      | HTTP Headers sent before the request body. Maps keys to header values like `{:Content-Type "text/html"}`                                          |
| body    | any      | nil     | HTTP Response body to return. Strings are sent as-is but other middleware can be added to parse and stringify JSON data or support other formats. |

### Async Promise Handling

While middleware looks synchronous, request handlers can return promises for
async tasks. This should allow users to work with promesa which comes with
node-babashka (nbb) out of the box. For example:

```clojure
(require '[promesa.core :as p])

(defn my-async-mw
  [next]
  (fn [req]
    (p/let [user (db/fetch-user)]
      (next (assoc req :user user)))))
```

It is not recommended to layer on many async requests into the ring
middleware. Doing so may result in very poor performance as the request will take
the total time of every process in the middleware stack to resolve.

## Middleware

### Query String Parsing

Parses the incoming `(:query-string req)` into a cljs hash-map. Associates the
`:query` hash-map onto the `req` for the next middleware to access as needed.

### JSON Parsing and Encoding

Define a json middleware that parses incoming request body data into JSON then
uses `js->clj` to convert to edn. It should only parse when content-type is set
to `application/json`. At the end of the request chain it should also
serialize body data if content-type of the response is `application/json`.

### Static File Serving

Define static file middleware that appropriately loads the requested file, reads
the contents, determines best fit mime-type, and sends the matching content-type
header with the contents as the body.

It should move on to the next middleware if the uri does not contain an
extension.

A 404 should be returned if the file does not exist. A 500 should be returned if
there is an error reading the file.

## REPL Driven Development

### Evaluate Individual Middleware

An optimal REPL experience is the ability to evaluate individual middleware
definitions that would take effect for any incoming requests after eval. This
makes it trivial to draft new middleware, change routes, and fix bugs without
having to restart the server every time.

The discovered approach so far is to have the ring middleware stack evaluated
every request and instead of referencing functions directly like
`(my-lib/my-mw)` reference the name indirectly like `(#'my-lib/my-mw)`. This
enables evaluating a single middleware function and the changes are applied by
the next incoming request. Otherwise devs would need to eval the updated
middleware, eval the ring middleware stack, and restart the server.

### List Servers

Servers should be tracked in an atom so they can be referenced even if a server
instance is not stored in a named symbol. This wil prevent servers from getting
lost in the background when evaluating in a REPL.

### Start, Stop, and Restart Servers

Given the ability to list servers, there should be utility functions users can
use in the REPL to start a stopped server, stop a started server, and restart a
stopped or started server.

## Out-of-scope Features

### Caching

While typicially useful in a production server, there are too many ways to
tackle caching and all of which come with a lot of trade offs. This is best left
to separate middleware packages.

### Hiccup Templating

This library will not provide any templating functionality such as HTML files,
Handlebars, or Hiccup rendering. It is relatively trivial to setup using
Reagent's server-side rendering APIs, however this library should not be tied to
any particular rendering library or approach but an example may be included in
the docs to show how to set it up.

### Routing

To promote flexibility it will be beneficial to not enforce any particular
routing implementation. This is consistent with Ring's Clojure implementation
which can be used with libraries like Reitit or Compojure. That said, there is
room for creating a routing library that provides a clojure friendly API around
a trie based routing engine for better performance.

### Web Sockets

While worth exploring to see what it would take to support websocket
connections, v1 should focus on the core middleware.
