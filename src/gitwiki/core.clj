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
(defn nil??
  "Returns y if x is nil, x otherwise."
  [x y]
  (if (nil? x) y x))

(defn authenticated? 
  [name pass]
  (and (= name "test") (= pass "test") "test"))

(defmacro page_url
  [page]
  `(str "/wiki/" ~page))

(defmacro edit_url
  [page]
  `(str "/edit/" ~page))

(defmacro history_url
  ([] `(str "/history"))
  ([page] `(str "/history/" ~page)))

;; the pages
(en/deftemplate view
  (en/xml-resource (str THEME "/view.html"))
  [page & {user :user}]
  [:title] (en/content [PROJECT " > " page])
  [:a.home_url] (en/set-attr :href (page_url DEFAULT_PAGE))
  [:a.global_history_url] (en/set-attr :href (history_url))
  [:span.username] (en/content ["| Logged in as " user])
  [:h1#title] (en/content [page])
  [:a.edit_url] (en/set-attr :href (edit_url page))
  [:a.history_url] (en/set-attr :href (history_url page)))

(en/deftemplate history
  (en/xml-resource (str THEME "/history.html"))
  [page & {user :user}]
  [:title] (en/content [PROJECT " > History of " (nil?? page "all")])
  [:a.home_url] (en/set-attr :href (page_url DEFAULT_PAGE))
  [:a.global_history_url] (en/set-attr :href (history_url))
  [:span.username] (en/content ["| Logged in as " user])
  [:h1#title] (en/content ["History of " (nil?? page "all")]))

;; the handlers
(defroutes handler
  ;; view
  (GET "/" req (view DEFAULT_PAGE :user (:basic-authentication req)))
  (GET "/wiki/:page" [page :as req] (view page :user (:basic-authentication req)))
  ;; edit
  ;; history
  (GET "/history" req (history nil :user (:basic-authentication req)))
  (GET "/history/:page" [page :as req] (history page :user (:basic-authentication req)))
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
