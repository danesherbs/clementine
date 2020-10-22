(ns clementine.core
  "Lightweight web crawler with an aritrary handler."
  (:use [clojure.core.async :only (go)])
  (:require [clojure.core.cache :as cache]
            [net.cgrand.enlive-html :as html]
            [clj-http.client :as client]))

(def http-opts {:socket-timeout 10000
                :conn-timeout 10000
                :insecure? false
                :cookie-policy :standard
                :throw-entire-message? false})

; (fetch-dom "https://news.ycombinator.com")
; (fetch-dom "https://github.com/HackerNews/API")
(defn fetch-dom
  "Retrieves DOM at given url"
  [url]
  (html/html-snippet
   (:body (client/get url http-opts))))

; (resolve-path "https://news.ycombinator.com" "news?p=3")
; (resolve-path "https://news.ycombinator.com/news?p=2" "news?p=3")
; (resolve-path "https://news.ycombinator.com/news?p=2" "https://worksinprogress.co/#issue-Issue 2")
(defn resolve-path [url other]
  (try (java.net.URL. (java.net.URL. url) other)
       (catch java.net.MalformedURLException e (println "Couldn't join" url "and" other))))

; (.uncaughtException (java.net.URL. "javascript.void(0)"))
(defn is-http
  [url]
  (try (.startsWith (.getProtocol url) "http")
       (catch Exception e (println "Couldn't get protocol of" url) false)))

(is-http (java.net.URL. "tel:/hello"))

(defn fetch-urls
  "Fetches urls on page"
  [url link-selector]
  (-> url
      (as-> url (try (fetch-dom url) (catch Exception e (println "Couldn't fetch" url (.getMessage e)) [])))
      (html/select link-selector)
      (as-> nodes (map :attrs nodes))
      (as-> attrs (map :href attrs))
      (as-> hrefs (remove nil? hrefs))
      (as-> hrefs (map (fn [href] (resolve-path url href)) hrefs))
      (as-> hrefs (filter is-http hrefs))
      (as-> hrefs (map str hrefs))))

; URLs currently being visited
(def visiting (ref (set nil)))

; URLs visited
(def visited (ref (set nil)))

; (crawl "https://news.ycombinator.com" println [[:a.morelink]] 10)
; (crawl "https://news.ycombinator.com" println [[:a]] 1)
; (crawl "https://google.com" println [[:a]] 2)
; (crawl "https://gocardless.com/en-au/" println [[:a]] 2)
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
             (when (and (not (contains? @visiting next-url))
                        (not (contains? @visited next-url)))
               (ref-set crawl-next-url true)
               (alter visiting disj url)))
            ; Crawl next url
            (when @crawl-next-url
              (go (crawl next-url handler link-selector (dec max-depth))))))))))

; (crawl "https://news.ycombinator.com" (fn [url] (spit "output.txt" (str url "\n") :append true)) [[:a]] 1)
; (crawl "https://gocardless.com/en-au/" (fn [url] (spit "output.txt" (str url "\n") :append true)) [[:a]] 3)
