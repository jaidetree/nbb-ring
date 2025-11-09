(ns dev.jaide.tests.run
  (:require
   [clojure.test :as t]
   [dev.jaide.nbb-ring.colors :as c]
   [clojure.pprint :refer [pprint]]
   [dev.jaide.nbb-ring.server-test]))

(def default-state {:pass 0 :fail 0 :error 0 :errors []})
(def state (atom default-state))

(defn reset-state!
  []
  (reset! state default-state))

(defn inc-var-count!
  [k]
  (swap! state update k inc))

(defn append-error!
  [error-str]
  (swap! state update :errors conj error-str))

(defn print-comparison [m]
  (let [formatter-fn (or (:formatter (t/get-current-env)) pr-str)]
    (println "expected:" (formatter-fn (:expected m)))
    (println "  actual:" (formatter-fn (:actual m)))))

(defmethod t/report [:cljs.test/default :begin-test-ns] [m]
  (println "\nTesting" (ns-name (:ns m)))
  (println))

(defmethod t/report [:cljs.test/default :begin-test-var] [m]
  (reset-state!)
  (js/process.stdout.write (.padEnd (str "  " (-> m :var meta :name) " ")
                                    50 ".")))

(defmethod t/report [:cljs.test/default :pass] [m]
  (t/inc-report-counter! :pass)
  (inc-var-count! :pass))

(defmethod t/report  [:cljs.test/default :fail] [m]
  (t/inc-report-counter! :fail)
  (inc-var-count! :fail)
  (append-error!
   (with-out-str
     (println (str "\n" (c/red "FAILURE") " in") (t/testing-vars-str m))
     (when (seq (:testing-contexts (t/get-current-env)))
       (println (t/testing-contexts-str)))
     (when-let [message (:message m)] (println message))
     (print-comparison m)
     (println))))

(defmethod t/report [:cljs.test/default :error] [m]
  (t/inc-report-counter! :error)
  (inc-var-count! :error)
  (append-error!
   (with-out-str
     (println "\nERROR in" (t/testing-vars-str m))
     (when (seq (:testing-contexts (t/get-current-env)))
       (println (t/testing-contexts-str)))
     (when-let [message (:message m)] (println message))
     (print-comparison m)
     (println))))

(defmethod t/report [:cljs.test/default :end-test-var] [m]
  (let [{:keys [pass fail error errors]} @state
        total (+ pass fail error)
        not-pass (+ fail error)]
    (if (zero? not-pass)
      (println (str " [" (c/green "passed " pass) "/" (c/green total) "]"))
      (println (str " [" (c/red "failed " not-pass) "/" (c/red total) "]")))
    (doseq [error errors]
      (println error))))

(defn -main
  []
  (t/run-all-tests #".*-test$")
  nil)
