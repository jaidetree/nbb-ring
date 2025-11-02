(ns dev.jaide.nbb-ring.server
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :as s]
   [promesa.core :as p]
   ["http" :as http]))

(defn- server-host
  [request]
  (let [headers (.-headers request)
        host (aget headers "host")
        [hostname port] (s/split host #":")]
    {:full host
     :hostname hostname
     :port (if (s/blank? port)
             nil
             (js/Number.parseInt port 10))}))

(defn- request-protocol
  [request]
  (if (some-> request (.-socket) (.-encrypted))
    "https"
    "http"))

(defn- request-content-type
  [headers]
  (let [content-type (aget headers "content-type")
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

(defn node-req->ring
  [request]
  (let [headers (.-headers request)
        controller (js/AbortController.)
        protocol (request-protocol request)
        host (server-host request)
        url (js/URL. (str protocol "://" (:full host) (.-url request)))
        content-type (request-content-type headers)]
    (js/console.log url)
    {:server-port        (get host :port)
     :server-name        (get host :hostname)
     :remote-addr        (.. request -socket (address) (-address)) ;; @TODO X-Forwarded-For headers
     :uri                (.-url request)
     :url                (js/String url)
     :query-string       (.-search url)
     :query              (searchParams->clj (.-searchParams url))
     :scheme             (keyword protocol)
     :request-method     (or  (-> request
                                  (.-method)
                                  (s/lower-case)
                                  (keyword))
                              :get)
     :protocol           (str (s/upper-case protocol) "/" (.-httpVersion request))
     :headers            (js->clj (.-headers request) :keywordize-keys true)
     :content-type       (get content-type :type)
     :content-length     (aget headers "Content-Length")
     :character-encoding (get content-type :charset)
     :ssl-client-cert    {}
     :body               (.-body request)
     :signal             (.-signal controller)}))

(defn request-listener
  [ring-mw]
  (fn [node-req node-res]
    (js/console.log (.-headers node-req))
    (let [req (node-req->ring node-req)]
      (pprint req)
      (.end node-res "Hello World"))))

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

