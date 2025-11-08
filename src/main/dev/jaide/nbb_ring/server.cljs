(ns dev.jaide.nbb-ring.server
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :as s]
   [promesa.core :as p]
   [dev.jaide.nbb-ring.middleware :as mw]
   ["net" :as net]
   ["http" :as http]
   ["crypto" :as crypto]))

(defn- parse-server-host
  [request]
  (let [headers (.-headers request)
        host (aget headers "host")
        [hostname port] (s/split host #":")]
    {:full host
     :hostname hostname
     :port (if (s/blank? port)
             80
             (js/Number.parseInt port 10))}))

(defn- get-request-protocol
  [request]
  (if-let [proxy-proto (.. request -headers -x-forwarded-proto)]
    proxy-proto
    (if (some-> request (.-socket) (.-encrypted))
      "https"
      "http")))

(defn- parse-content-type
  [request]
  (let [content-type (.. request -headers -content-type)
        [type-str] (->> (s/split content-type #";")
                        (map s/trim))]
    {:type type-str
     :charset (let [charset (-> content-type
                                (s/split #"charset=")
                                (second)
                                (s/split #";")
                                (first))]
                (if (s/blank? charset)
                  nil
                  charset))}))

(defn- get-content-length
  [request]
  (let [length (-> request
                   (.-headers)
                   (.-content-length)
                   (js/Number.parseInt 10))]
    (when (not (js/Number.isNaN length))
      length)))

(defn- get-request-method
  [request]
  (let [method (-> request (.-method))]
    (if (s/blank? method)
      :get
      (-> method
          (s/lower-case)
          (keyword)))))

(defn- get-client-cert
  "
  @TODO This function is untested
  "
  [request]
  (when (.. request -socket -getPeerCertificate)
    (let [cert (.. request -socket (getPeerCertificate true) -raw)]
      (new (.-X509Certificate crypto) cert))))

(defn- get-client-address
  [request]
  (let [socket-ip (.. request -socket -remoteAddress)
        ips-str (.. request -headers -x-forwarded-for)]
    (if ips-str
      (let [ip (->> (s/split ips-str #",")
                    (map s/trim)
                    (filter (.-isIP net))
                    (first))]
        (if (s/blank? ip)
          socket-ip
          ip))
      socket-ip)))

(defn node-req->ring
  "
  Transform node request into a ring-like request hash-map

  Takes a http.IncomingMessage
  Returns a request hash-map
  "
  [request]
  (let [controller (js/AbortController.)
        protocol (get-request-protocol request)
        host (parse-server-host request)
        url (js/URL. (str protocol "://" (:full host) (.-url request)))
        content-type (parse-content-type request)
        method (get-request-method request)]
    {:server-port        (get host :port)
     :server-name        (get host :hostname)
     :remote-addr        (get-client-address request)
     :uri                (.-url request)
     :url                (js/String url)
     :query-string       (.-search url)
     :scheme             (keyword protocol)
     :request-method     method
     :method             method
     :protocol           (str (s/upper-case protocol) "/" (.-httpVersion request))
     :headers            (js->clj (.-headers request) :keywordize-keys true)
     :content-type       (get content-type :type)
     :content-length     (get-content-length request)
     :character-encoding (get content-type :charset)
     :ssl-client-cert    (get-client-cert request)
     :body               (.-body request)
     :signal             (.-signal controller)}))

(def ^:private default-method-statuses
  {"POST"    201
   "DELETE"  204
   "PUT"     200
   "PATCH"   200
   "GET"     200
   ;; Some browsers incorrectly assume 204 No Content applies to the resource
   ;; and do not send a subsequent request to fetch it
   ;; https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Methods/OPTIONS#specifications
   "OPTIONS" 200})

(defn send-node-response
  "
  Applies ring res hash-map fields to the node-response object

  Supported properties:
  - status  {number} - Response status code, defaults to 200
  - headers {hash-map} - Headers mapping keywords or strings to header values
  - timeout {number} - Set socket timeout on individual request
  - body    {string} - Response body

  Takes a node http.ServerResponse and a ring res hash-map
  Returns nil
  "
  [response res]
  (set! (.-statusCode response)
        (or (get res :status)
            (when (nil? (:body res)) 204)
            (get default-method-statuses (.. response -req -method))))
  (when (map? (:headers res))
    (doseq [[header value] (get res :headers {})]
      (.setHeader response
                  (if (keyword? header) (name header) header)
                  value)))
  (when (:timeout res)
    (.setTimeout res (get :res :timeout)))
  (.end response (when (:body res)
                   (get res :body ""))))

(defn create-request-handler
  [ring-mw]
  (fn [node-req node-res]
    (if (= (.-url node-req) "/favicon.ico")
      (.end node-res "")
      (let [req (node-req->ring node-req)]
        (p/catch
         (p/let [res (ring-mw req)]
           (send-node-response node-res res)
           #_(.end node-res "Hello World"))
         (fn [err]
           (js/console.error err)
           (set! (.-statusCode node-res) 500)
           (.end node-res (js/String err))))))))

(defn server-callback
  [server]
  (let [addr-obj (.address server)]
    (println (str "Server is listening on http://" (.-address addr-obj) ":" (.-port addr-obj)))))

(defn hello-mw
  [_next]
  (fn [req]
    {:status 200
     :req-id (:id req)
     :body (js/JSON.stringify (clj->js {:status :ok
                                        :message "Hello World"}))
     :headers {:Content-Type "application/json"}}))

(defn default-mw
  [_req]
  {:status 404
   :body "Not Found"})

(defn stop-server
  [server]
  (.close server)
  (js/process.exit 0))

(defn -main
  []
  (p/let [mw-fn (p/-> default-mw
                      (hello-mw)
                      (mw/pprinter)
                      (mw/query-parser)
                      (mw/logger)
                      (mw/identifier))]
    (let [server (http/createServer (create-request-handler mw-fn))]
      (.listen server 3030 "0.0.0.0" #(server-callback server))
      (doseq [signal ["SIGINT" "SIGTERM" "SIGQUIT"]]
        (js/process.on signal #(stop-server server))))))

