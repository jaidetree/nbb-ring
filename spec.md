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

### Middleware

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

### Headers and Timeouts

## Ring Adapter

### Ring Interface

#### Request

#### Response

### Async Promise Handling

## Middleware

### Caching

### JSON Parsing and Encoding

### Static File Serving

## Out-of-scope Features

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
