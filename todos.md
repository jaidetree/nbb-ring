# Tasks

## Server

- [x] Create http server
- [ ] Clustering
- [ ] Timeout
- [ ] KeepAliveTimeout
- [ ] TimeoutBuffer
- [ ] HeadersTimeout
- [ ] Graceful shutdown
  - [ ] Handle SIGTERM events
  - [ ] Handle SIGINT events
- [ ] Handle uncaught errors
- [ ] Handle unhandled rejections
- [ ] Wrap Body with abortcontroller controlled stream

### Server Options & Env Vars

- [ ] Timeout
- [ ] KeepAliveTimeout
- [ ] TimeoutBuffer
- [ ] HeadersTimout
- [ ] Port
- [ ] Host

## REPL Support

- [ ] Call middleware factory every request in development
- [ ] Cache the middleware factory output in production

## Requests

- [x] server-port
- [x] server-name
- [x] remote-addr
  - [x] Support x-forwarded-for IPs
- [x] uri
- [x] query-string
- [x] scheme
- [x] request-method
- [x] protocol
- [x] headers
- [x] content-type
- [x] content-length
- [x] character-encoding
- [x] ssl-client-cert
- [x] body
  - [ ] Wrap with abort signal
- [ ] Catch handler
  - [ ] Format error messages nicely

## Responses

- [x] Support status codes
- [x] Support headers
- [x] Support body strings

### Body Streams

- [ ] stream.Readable
- [ ] web.ReadableStream

## Testing

- [ ] Conjure up a test-runner script
- [ ] Look for previous custom reporting

### Tests

- [ ] Request handling
  - [ ] Test x-forwarded-for header
  - [ ] Test x-forwarded-proto header
  - [ ] Test abort signal
- [ ] Response handling
  - [ ] Test status
- [ ] Middleware
  - [ ] Synchronous
  - [ ] Asynchronous
- [ ] Prod middleware handling
- [ ] Dev middleware handling

## Middleware

- [ ] Basic Logging
  - [ ] Colors
  - [ ] Disable colors via env var or when not a TTY
- [ ] Print request details
- [ ] Query string parsing
- [ ] Identifier
- [ ] JSON Parsing & Serialization
