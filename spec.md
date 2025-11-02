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
