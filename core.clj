(ns clementine.core
  "Lightweight web crawler with an aritrary handler."
  (:use [clojure.core.async :only (go)])
  (:require [clojure.core.cache :as cache]
            [net.cgrand.enlive-html :as html]
            [clj-http.client :as client]
            [clojurewerkz.urly.core :as urly]))

; (fetch-dom "https://news.ycombinator.com")
(defn fetch-dom
  "Retrieves DOM at given url"
  [url]
  (-> url
      client/get :body
      html/html-snippet))

; (fetch-urls "https://news.ycombinator.com" [[:a.morelink]])
(defn fetch-urls
  "Fetches urls on page"
  [url link-selector]
  (-> url
      fetch-dom
      (html/select link-selector)
      (as-> nodes (map :attrs nodes))
      (as-> nodes (map :href nodes))
      (as-> nodes (map (fn [href]
                         (str (urly/resolve
                               (urly/url-like url)
                               (urly/url-like href)))) nodes))))

; URLs currently being visited
(def visiting (ref (set nil)))

; URLs visited
(def visited (ref (set nil)))

; (crawl "https://news.ycombinator.com" println [[:a.morelink]] 10)
(defn crawl
  "Passes all reachable urls from url to handler fn"
  [url handler link-selector max-depth]
  (if (>= max-depth 0)
    ; Crawl if not visted
    (let [crawl-url (ref false)]
      ; Mark current url as visited
      (dosync
       (when-not (contains? @visited url)
         (ref-set crawl-url true)
         (alter visited conj url)
         (alter visiting disj url)))
      ; Start crawl
      (when @crawl-url
        ; Pass url to handler
        (go (handler url))
        ; Crawl all urls on current page
        (doseq [next-url (fetch-urls url link-selector)]
          ; Crawl next url if not visited or soon to be
          (let [crawl-next-url (ref false)]
            ; Mark current url as visiting
            (dosync
             (when-not (and (contains? @visiting next-url)
                            (contains? @visited next-url))
               (ref-set crawl-next-url true)
               (alter visiting disj url)))
            ; Crawl next url
            (when @crawl-next-url
              (go (crawl next-url handler link-selector (dec max-depth))))))))))