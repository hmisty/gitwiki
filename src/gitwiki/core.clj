(ns gitwiki.core
  (:gen-class)
  (:import [java.io FileNotFoundException])
  (:use [clojure.pprint]
        [compojure.core :only (GET POST defroutes)]
        [ring.middleware.basic-authentication])
  (:require [clojure.string :as string]
            [ring.util.response :as resp]
            [net.cgrand.enlive-html :as en]
            [compojure.handler]
            [compojure.route]
            [clojure.java.io :as io]
            [gitwiki.textile :as textile]))

;; some configs
(def PROJECT "GitWiki")
(def DEFAULT_PAGE "Home")
(def THEME "default")
(def DATA_DIR "data")

;; helpers
(defn nil??
  "Returns y if x is nil, x otherwise."
  [x y]
  (if (nil? x) y x))

(defn authenticated? 
  "Returns the logged in username if authenticated"
  [name pass]
  (and (= name "test") (= pass "test") "test"))

(defmacro page_url
  "Returns the URL to view the page"
  [page]
  `(str "/wiki/" ~page))

(defmacro edit_url
  "Returns the URL to edit the page"
  [page]
  `(str "/edit/" ~page))

(defmacro history_url
  "Returns the URL to the history of the page"
  ([] `(str "/history"))
  ([page] `(str "/history/" ~page)))

(defmacro page_file
  "Returns the file path for the wiki page"
  [page]
  `(str DATA_DIR "/" ~page))

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
  [:a.history_url] (en/set-attr :href (history_url page))
  [:div#content] (en/html-content (try (textile/parse (slurp (page_file page)))
                               (catch FileNotFoundException e "")))
  [:span#last_modified] (en/content "XXXX-XX-XX XX:XX:XX")) ;TODO

(en/deftemplate edit
  (en/xml-resource (str THEME "/edit.html"))
  [page & {user :user}]
  [:title] (en/content [PROJECT " > Editing " page])
  [:a.home_url] (en/set-attr :href (page_url DEFAULT_PAGE))
  [:a.global_history_url] (en/set-attr :href (history_url))
  [:span.username] (en/content ["| Logged in as " user])
  [:h1#title] (en/content ["Editing " page])
  [:form] (en/set-attr :action (page_url page))
  [:textarea#content] (en/content (try (slurp (page_file page)) 
                                    (catch FileNotFoundException e ""))))

(en/deftemplate history
  (en/xml-resource (str THEME "/history.html"))
  [page & {user :user}]
  [:title] (en/content [PROJECT " > History of " (nil?? page "all")])
  [:a.home_url] (en/set-attr :href (page_url DEFAULT_PAGE))
  [:a.global_history_url] (en/set-attr :href (history_url))
  [:span.username] (en/content ["| Logged in as " user])
  [:h1#title] (en/content ["History of " (nil?? page "all")]))

;; the action
(defn save
  [req page]
  (let [{user :basic-authentication
         {input "data"} :form-params} req]
    (with-open [w (io/writer (page_file page))] ;TODO FileNotFoundException
      (.write w input)))
  (resp/redirect (page_url page)))

;; the handlers
(defroutes handler
  ;; view
  (GET "/" req (view DEFAULT_PAGE :user (:basic-authentication req)))
  (GET "/wiki/:page" [page :as req] (view page :user (:basic-authentication req)))
  (POST "/wiki/:page" [page :as req] (save req page))
  ;; edit
  (GET "/edit/:page" [page :as req] (edit page :user (:basic-authentication req)))
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
      (wrap-basic-authentication authenticated?)
      (compojure.handler/site)))
