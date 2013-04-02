(ns gitwiki.core
  (:gen-class)
  (:import [java.io FileNotFoundException])
  (:use [clojure.pprint]
        [compojure.core :only (GET POST defroutes)]
        [ring.middleware.basic-authentication]
        [gitwiki.git])
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
(defn authenticated? 
  "Returns the logged in username if authenticated."
  [name pass]
  (and (= name "test") (= pass "test") "test"))

(defmacro page-url
  "Returns the URL to view the page."
  [page]
  `(str "/wiki/" ~page))

(defmacro edit-url
  "Returns the URL to edit the page."
  [page]
  `(str "/edit/" ~page))

(defmacro history-url
  "Returns the URL to the history of the page."
  ([] `(str "/history"))
  ([page] `(str "/history/" ~page)))

(defmacro page-file
  "Returns the file path for the wiki page."
  [page]
  `(str DATA_DIR "/" ~page))

(defn parse
  "Returns the parsed html from the content in textile."
  [content]
  (let [page-link (fn [page]
                    (string/join "" (en/emit* (en/html [:a {:href (page-url page)} page]))))]
    (string/replace (textile/parse content) #"\[([A-Z]\w+)\]" (page-link "$1"))))

(defn file-modified-time
  "Returns the formatted date time of the last modified time of the specified file."
  [file]
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss") (java.util.Date. (.lastModified (io/file file)))))

(defn commit-time
  "Returns the formaated date time of the commit."
  [commit]
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss")
           (java.util.Date. (* 1000 (-> commit .getCommitTime)))))
  

;; the pages
(en/deftemplate view'
  (en/xml-resource (str THEME "/view.html"))
  [page & {user :user}]
  [:title] (en/content [PROJECT " > " page])
  [:a.home_url] (en/set-attr :href (page-url DEFAULT_PAGE))
  [:a.global_history_url] (en/set-attr :href (history-url))
  [:span.username] (en/content ["| Logged in as " user])
  [:h1#title] (en/content [page])
  [:a.edit_url] (en/set-attr :href (edit-url page))
  [:a.history_url] (en/set-attr :href (history-url page))
  [:div#content] (en/html-content (try (parse (slurp (page-file page)))
                                    (catch FileNotFoundException e "")))
  [:span#last_modified] (en/content (file-modified-time (page-file page)))) ;TODO

(defn view
  [page & {user :user}]
  (if (.exists (io/file (page-file page)))
    (view' page :user user)
    (resp/redirect (edit-url page))))

(en/deftemplate edit
  (en/xml-resource (str THEME "/edit.html"))
  [page & {user :user}]
  [:title] (en/content [PROJECT " > Editing " page])
  [:a.home_url] (en/set-attr :href (page-url DEFAULT_PAGE))
  [:a.global_history_url] (en/set-attr :href (history-url))
  [:span.username] (en/content ["| Logged in as " user])
  [:h1#title] (en/content ["Editing " page])
  [:form] (en/set-attr :action (page-url page))
  [:textarea#content] (en/content (try (slurp (page-file page)) 
                                    (catch FileNotFoundException e ""))))

(en/deftemplate history
  (en/xml-resource (str THEME "/history.html"))
  [page & {user :user}]
  [:title] (en/content [PROJECT " > History of " (or page "all")])
  [:a.home_url] (en/set-attr :href (page-url DEFAULT_PAGE))
  [:a.global_history_url] (en/set-attr :href (history-url))
  [:span.username] (en/content ["| Logged in as " user])
  [:h1#title] (en/content ["History of " (or page "all")])
  [:#history :tr.commit]
  (let [g (git DATA_DIR)
        commits (g :log)] ;; FIXME no HEAD of first created repo will cause an exception
    (en/clone-for [ci commits]
                  [[:td (en/attr= :name "date")]] (en/content (commit-time ci))
                  [[:td (en/attr= :name "author")]] (en/content (-> ci .getAuthorIdent .getName))
                  [[:a (en/attr= :name "page")]]
                  (comp
                    (en/content "Home")
                    (en/set-attr :href (page-url "Home")))
                  [[:a (en/attr= :name "view")]] (en/set-attr :href "ci-view-link"))))

;; the action
(defn save
  [req page]
  (let [{user :basic-authentication
         {input "data"} :form-params} req]
    (with-open [w (io/writer (page-file page))] ;TODO FileNotFoundException e.g. without data dir
      (.write w input)))
  (resp/redirect (page-url page)))

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
