# Clementine
[![Clojars Project](https://img.shields.io/clojars/v/clementine.svg)](https://clojars.org/clementine)

A lightweight concurrent web crawler with minimal dependencies

### Hacker News headlines
```clojure
(require '[net.cgrand.enlive-html :as html])

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
        :link-selector [:a.morelink]
        :max-depth 3})
```

### Amazon reviews
```clojure
(require '[net.cgrand.enlive-html :as html])

(def amazon-url "https://www.amazon.ca/Echo-Dot-3rd-gen-Charcoal/product-reviews/B07PDHT5XP/ref=cm_cr_dp_d_show_all_btm?ie=UTF8&reviewerType=all_reviews")

(defn reviews
  "Retrieves Amazon reviews"
  [url]
  (-> url
      fetch-dom
      (html/select [:div.review :span.review-text])
      (as-> reviews (map html/text reviews))
      (as-> reviews (map clojure.string/trim reviews))
      println))

; Retrieve Amazon reviews on first few pages!
(crawl {:url amazon-url
        :handler reviews
        :link-selector [:li.a-last :a]
        :max-depth 3})
```

### Setup

Leiningen/Boot
```clojure
[clementine "0.0.1"]
```

Clojure CLI/deps.edn
```clojure
clementine {:mvn/version "0.0.1"}
```

Gradle
```gradle
compile 'clementine:clementine:0.0.1'
```

Maven
```xml
<dependency>
  <groupId>clementine</groupId>
  <artifactId>clementine</artifactId>
  <version>0.0.1</version>
</dependency>
```
