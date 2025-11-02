(ns dev.jaide.nbb-ring.server
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :as s]
   [promesa.core :as p]
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

(defn- searchParams->clj
  [searchParams]
  (-> searchParams
      (.entries)
      (js/Object.fromEntries)
      (js->clj :keywordize-keys true)))

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
        content-type (parse-content-type request)]
    {:server-port        (get host :port)
     :server-name        (get host :hostname)
     :remote-addr        (get-client-address request)
     :uri                (.-url request)
     :url                (js/String url)
     :query-string       (.-search url)
     :query              (searchParams->clj (.-searchParams url))
     :scheme             (keyword protocol)
     :request-method     (get-request-method request)
     :protocol           (str (s/upper-case protocol) "/" (.-httpVersion request))
     :headers            (js->clj (.-headers request) :keywordize-keys true)
     :content-type       (get content-type :type)
     :content-length     (get-content-length request)
     :character-encoding (get content-type :charset)
     :ssl-client-cert    (get-client-cert request)
     :body               (.-body request)
     :signal             (.-signal controller)}))

(defn request-listener
  [ring-mw]
  (fn [node-req node-res]
    (if (= (.-url node-req) "/favicon.ico")
      (.end node-res "")
      (let [req (node-req->ring node-req)]
        (pprint req)
        (.end node-res "Hello World")))))

(defn server-callback
  [server]
  (let [addr-obj (.address server)]
    (println (str "Server is listening on http://" (.-address addr-obj) ":" (.-port addr-obj)))))

(defn stop-server
  [server]
  (.close server)
  (js/process.exit 0))

(defn -main
  []
  (let [server (http/createServer (request-listener))]
    (.listen server 3030 "0.0.0.0" #(server-callback server))
    (doseq [signal ["SIGINT" "SIGTERM" "SIGQUIT"]]
      (js/process.on signal #(stop-server server)))))

