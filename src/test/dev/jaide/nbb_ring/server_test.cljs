(ns dev.jaide.nbb-ring.server-test
  (:require
   [cljs.test :as t :refer [deftest testing is]]))

(deftest server-test
  (testing "server"
    (is (= 2 1))))
