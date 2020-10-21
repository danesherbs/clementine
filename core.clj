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
  (try
    (-> url
        client/get :body
        html/html-snippet)
    (catch Exception e
      ; (println "Couldn't fetch" url (.getMessage e))
      (list))))  ; return empty dom

(defn resolve-urls
  "Resolves base url and a relative link"
  [base rel]
  (if (urly/relative? rel)
    (str (.mutatePath (urly/url-like base) rel))
    (try
      (str (urly/resolve
            (urly/url-like base)
            (urly/url-like rel)))
      (catch Exception e))))
      ; (println "Couldn't resolve" base "and" rel (.getMessage e)))))

(defn fetch-urls
  "Fetches urls on page"
  [url link-selector]
  (-> url
      fetch-dom
      (html/select link-selector)
      (as-> nodes (map :attrs nodes))
      (as-> attrs (map :href attrs))
      (as-> hrefs (remove nil? hrefs))
      (as-> hrefs (map (fn [href] (resolve-urls url href)) hrefs))
      (as-> hrefs (remove nil? hrefs))))

; URLs currently being visited
(def visiting (ref (set nil)))

; URLs visited
(def visited (ref (set nil)))

; (crawl "https://news.ycombinator.com" println [[:a.morelink]] 10)
; (crawl "https://google.com" println [[:a]] 2)
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
