(ns clementine.core
  "Lightweight web crawler with an aritrary handler."
  (:use [clojure.core.async :only (go)])
  (:require [clojure.core.cache :as cache]
            [net.cgrand.enlive-html :as html]
            [clj-http.client :as client]))


; Default config for http requests


(def http-opts {:socket-timeout 4000
                :conn-timeout 4000
                :insecure? false
                :cookie-policy :standard
                :throw-entire-message? false})

; -> (fetch-dom "https://news.ycombinator.com")
; -> ({:tag :a {:href "a.com"}} ...)
(defn fetch-dom
  "Retrieves DOM at given url"
  [url]
  (try (html/html-snippet (:body (client/get url http-opts)))
       (catch Exception e (println "Couldn't fetch" url (.getMessage e)) [])))

; -> (resolve-path "https://news.ycombinator.com/news?p=2" "news?p=3")
; -> "https://news.ycombinator.com/news?p=3"
(defn resolve-path [url other]
  "Tries to resolve url and child url"
  (try (java.net.URL. (java.net.URL. url) other)
       (catch java.net.MalformedURLException e (println "Couldn't resolve" url "and" other))))

; -> (is-http (java.net.URL. "mailto:/address@site.com"))
; -> false
(defn is-http
  "Returns true iff url is valid and uses http"
  [url]
  (try (.startsWith (.getProtocol url) "http")
       (catch Exception e (println "Couldn't get protocol of" url) false)))

; -> (fetch-urls "https://news.ycombinator.com/news?p=2" [[:a.storylink]])
; -> ("https://a.com" "https://b.com" ...)
(defn fetch-urls
  "Fetches urls on page"
  [url link-selector]
  (-> url
      fetch-dom
      (html/select link-selector)
      (as-> nodes (map :attrs nodes))
      (as-> attrs (map :href attrs))
      (as-> hrefs (remove nil? hrefs))
      (as-> hrefs (map (fn [href] (resolve-path url href)) hrefs))
      (as-> hrefs (remove nil? hrefs))
      (as-> hrefs (filter is-http hrefs))
      (as-> hrefs (map str hrefs))))

; -> (go-crawl "https://news.ycombinator.com" println #{[:a.storylink] [:a.morelink]} 3)
; -> "https://a.com"
;    "https://b.com"
;          ...
(defn go-crawl
  "Passes all reachable urls from url to handler fn"
  [url handler link-selector max-depth visiting visited]
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
             (when (and (not (contains? @visiting next-url))
                        (not (contains? @visited next-url)))
               (ref-set crawl-next-url true)
               (alter visiting disj url)))
            ; Crawl next url
            (when @crawl-next-url
              (go (go-crawl next-url handler link-selector (dec max-depth) visiting visited)))))))))

; -> (crawl {:url "https://news.ycombinator.com"
;            :handler println
;            :link-selector #{[:a.storylink] [:a.morelink]}
;            :max-depth 3})
; -> "https://a.com"
;    "https://b.com"
;          ...
(defn crawl
  [config]
  (let [url           (:url config)
        handler       (:handler config)
        link-selector (:link-selector config)
        max-depth     (:max-depth config)
        visiting      (ref (set nil))
        visited       (ref (set nil))]
    (go-crawl url handler link-selector max-depth visiting visited)))