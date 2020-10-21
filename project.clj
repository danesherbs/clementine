(defproject clementine "0.0.1"
  :description "A tiny web crawler in Clojure!"
  :main clementine.core
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "1.3.610"]
                 [enlive "1.1.6"]
                 [clj-http "3.10.3"]
                 [clojurewerkz/urly "1.0.0"]])