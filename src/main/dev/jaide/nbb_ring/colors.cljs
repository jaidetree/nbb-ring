(ns dev.jaide.nbb-ring.colors
  (:require
   [clojure.string :as s]))

(defn color
  [color strs]
  (let [s (s/join " " strs)]
    (str "\033[" color "m" s "\033[0m")))

(defn black
  [& strs]
  (color 30 strs))

(defn red
  [& strs]
  (color 31 strs))

(defn green
  [& strs]
  (color 32 strs))

(defn yellow
  [& strs]
  (color 33 strs))

(defn blue
  [& strs]
  (color 34 strs))

(defn magenta
  [& strs]
  (color 35 strs))

(defn cyan
  [& strs]
  (color 36 strs))

(defn white
  [& strs]
  (color 37 strs))

(defn default
  [& strs]
  (color 39 strs))
