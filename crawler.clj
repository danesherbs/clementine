(require '[net.cgrand.enlive-html :as enlive])
(require '[clj-http.client :as client])
(require '[clojure.core.async :as async])

(defn fetch-dom
  "Retrieves DOM at given url"
  [url]
  (-> url
      client/get :body
      enlive/html-snippet))

; (fetch-dom "https://www.google.com")


(def ^:dynamic *link-selector* [[:a]])

(defn fetch-urls
  "Fetches urls on page"
  [url]
  (-> url
      fetch-dom
      (enlive/select *link-selector*)
      (as-> nodes (map :attrs nodes))
      (as-> nodes (map :href nodes))
      (as-> nodes (map (fn [href] (str url href)) nodes))))

; (fetch-urls "https://www.google.com")


(defn crawl
  "Passes all reachable urls from url to callback fn"
  [url callback max-depth]
  (async/go (callback url))
  (doseq [next-url (fetch-urls url)]
    (async/go (if (> max-depth 0) (crawl next-url callback (dec max-depth))))))

; (crawl "https://www.google.com" println 2)