(ns gitwiki.core
  (:gen-class)
  (:use [clojure.pprint]
        [compojure.core :only (GET POST defroutes)]
        [ring.middleware.basic-authentication :as auth])
  (:require [ring.util.response :as resp]
            [net.cgrand.enlive-html :as en]
            [compojure.handler]
            [compojure.route]))

;; some configs
(def PROJECT "GitWiki")
(def DEFAULT_PAGE "Home")
(def THEME "default")

;; helpers
(defmacro page_url
  [page]
  `(str "/wiki/" ~page))

(defmacro history_url
  ([] (str "/history"))
  ([page] (str "/history/" page)))

(defn authenticated? [name pass]
  (and (= name "test") (= pass "test")))

;; the pages
(en/deftemplate view
  (en/xml-resource (str THEME "/view.html"))
  [& page]
  [:title] (en/content [PROJECT " > " page])
  [:a.home_url] (en/set-attr :href (page_url DEFAULT_PAGE))
  [:a.global_history_url] (en/set-attr :href (history_url)))

(en/deftemplate history
  (en/xml-resource (str THEME "/history.html"))
  [& page]
  [:title] (en/content [PROJECT " > History " page])
  [:a.home_url] (en/set-attr :href (page_url DEFAULT_PAGE))
  [:a.global_history_url] (en/set-attr :href (history_url)))

;; the handlers
(defroutes handler
  ;; view
  (GET "/" req (view))
  (GET "/wiki/:page" [page] (view page))
  ;; edit
  ;; history
  (GET "/history" req (history))
  (GET "/history/:page" [page] (history page))
  ;; static resources
  (compojure.route/resources "/" {:root THEME})
  ;; default route
  (GET "*" req
       ;; FIXME return 404 page
       {:status 200 :body (with-out-str (pprint req))}))

;; the web app
(def app
  (-> handler
      (compojure.handler/site)
      (wrap-basic-authentication authenticated?)))
