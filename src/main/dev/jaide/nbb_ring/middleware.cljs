(ns dev.jaide.nbb-ring.middleware
  (:require
   [clojure.string :as s]
   [clojure.pprint :refer [pprint]]
   [dev.jaide.nbb-ring.colors :as c]
   ["querystring" :as qs]))

(defn identifier
  [next]
  (fn identifier-handler
    [req]
    (next (assoc req :id (random-uuid)))))

(defn- query-str->clj
  [query-str]
  (-> (.parse qs (s/replace query-str #"^\?" ""))
      (js/Object.entries)
      (js/Object.fromEntries)
      (js->clj :keywordize-keys true)))

(defn query-parser
  [next]
  (fn query-parser-handler
    [req]
    (if (s/blank? (:query-string req))
      (next (assoc req :query {}))
      (next (assoc req :query (query-str->clj (:query-string req)))))))

(defn logger
  [next]
  (fn logger-handler
    [req]
    (let [start (js/performance.now)
          res (next req)
          end (js/performance.now)
          diff (- end start)]
      (println (str "[" (c/magenta (.. (js/Date.) (toISOString))) "]"
                    " "
                    (c/green (:request-method req))
                    " "
                    (:uri req)
                    " "
                    (c/cyan (str (.toFixed diff 3) "ms"))))
      res)))

(defn pprinter
  [next]
  (fn pprinter-handler
    [req]
    (pprint req)
    (doto (next req)
      (pprint))))
