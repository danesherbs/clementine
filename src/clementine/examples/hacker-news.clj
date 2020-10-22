(ns clementine.examples.hacker-news-crawler
  (:require [clementine.core :refer [fetch-dom crawl]
             [net.cgrand.enlive-html :as html]]))

(def hacker-news-url "https://news.ycombinator.com")

(defn headlines
  "Retrieves Hacker News headlines"
  [url]
  (-> url
      fetch-dom
      (html/select [:td.title :a.storylink])
      (as-> nodes (map html/text nodes))
      println))

; Retrieve Hacker News headlines on first few pages!
(crawl {:url hacker-news-url
        :handler headlines
        :links [[:a.morelink]]
        :max-depth 3})