(ns gitwiki.core
  (:gen-class)
  (:use [clojure.pprint]
        [compojure.core :only (GET POST defroutes)])
  (:require [ring.util.response :as resp]
            [net.cgrand.enlive-html :as en]
            [compojure.handler]
            [compojure.route]))

;; some configs
(def DEFAULT_PAGE "Home")
(def THEME "default")

;; the pages
(en/deftemplate view
  (en/xml-resource (str THEME "/view.html"))
  [page]
  [:title] (en/content page)
  [:home_url] (en/content "some url"))

;; the handlers
(defroutes handler
  ;; view
  (GET "/" req (view DEFAULT_PAGE))
  (GET "/:page" [page] (view page))
  ;; edit
  ;; history
  ;; static resources
  (compojure.route/resources "/" {:root THEME}))
  ;; default route
  (GET "*" req
       ;; FIXME return 404 page
       {:status 200 :body (with-out-str (pprint req))}))

;; the web app
(def app
  (-> handler
      (compojure.handler/site)))
