(ns gitwiki.core
  (:gen-class)
  (:import [java.io File FileNotFoundException])
  (:use [clojure.pprint]
        [compojure.core :only (GET POST defroutes)]
        [ring.middleware.basic-authentication]
        [ring.middleware.multipart-params]
        [gitwiki.git])
  (:require [clojure.string :as string]
            [ring.util.response :as resp]
            [net.cgrand.enlive-html :as en]
            [compojure.handler]
            [compojure.route]
            [clojure.java.io :as io]
            [gitwiki.textile :as textile]
            [gitwiki.markdown :as markdown]
            [gitwiki.auth :as auth]))

;; some configs
(def PROJECT "GitWiki")
(def DEFAULT_PAGE "Home")
(def THEME "bootstrap")
(def DATA_DIR "data")
(def UPLOAD_FILE_DIR "files")
(def HISTORY_LIMIT 50)
(def APPKEY "1495367953") ;; appkey for weibo. for testing only

;; helpers
(defn authenticated? 
  "Returns the logged in username if authenticated."
  [username password]
  (and (auth/is-valid-user? username password) username))

(defmacro page-url
  "Returns the URL to view the page."
  ([page] `(str "/page/" ~page))
  ([page commit] `(str "/page/" ~page "/" ~commit)))

(defmacro comment-url
  "Returns the URL to comment the page."
  [page]
  `(str "/comment/" ~page))

(defmacro edit-url
  "Returns the URL to edit the page."
  [page]
  `(str "/edit/" ~page))

(defmacro history-url
  "Returns the URL to the history of the page."
  ([] `(str "/history"))
  ([page] `(str "/history/" ~page)))

(defmacro attach-url
  "Return the URL to attach file"
  ([] `(str "/attach"))
  ([page] `(str "/attach/" ~page)))

(defmacro page-file
  "Returns the file path for the wiki page."
  [page]
  `(str DATA_DIR "/" ~page))

(defmacro upload-file
  ([page] `(str UPLOAD_FILE_DIR "/" ~page))
  ([page file] `(str UPLOAD_FILE_DIR "/" ~page "/" ~file)))

(defn get-parser
  "Returns the parser for the pagename."
  [pagename]
  (let [lang (or (last (re-find #".(md|markdown|textile)$" pagename)) "markdown")]
    (case lang
      "md"        markdown/parse
      "markdown"  markdown/parse
      "textile"   textile/parse)))

(defn file-modified-time
  "Returns the formatted date time of the last modified time of the specified file."
  [file]
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss") (java.util.Date. (.lastModified (io/file file)))))

(defn page-file-list
  "Returns the file list information of the specified page"
  [page]
  (map (fn [f] {:name (.getName f) 
                :size (str (.length f) "B") 
                :time (file-modified-time f) 
                :path (str "/files/" page "/" (.getName f))})
       (filter #(.isFile %)
               (.listFiles (File. (upload-file page))))))

(defn git-log-flatten
  "Flatten the results of (git :log)."
  [log & [page]]
  (reverse 
    (filter #(if page (= (:file %) page)
               true)
            (reduce (fn [coll {name :name, [author-name author-email] :author,
                               date :date, message :message, 
                               name-status :name-status}]
                      (reduce #(conj % {:name name :author-name author-name 
                                        :author-email author-email :date date 
                                        :message message :file (first %2)
                                        :change-type (second %2)}) 
                              coll name-status))
                    '() log))))

;; content renders
(defn git-content
  [page commit]
  (let [g (git DATA_DIR)
        file-content (if commit (g :cat-file page commit)
                       (g :cat-file page))
        parse (get-parser page)]
    (-> file-content parse en/html-snippet
        (en/transform [:h1] (fn [x] (assoc (first (en/html [:div.page-header])) :content (list x)))))))

(defn view-content
  [page commit user]
  (en/at
    (en/html-resource (str THEME "/view.html"))
    [:div#content] (en/content (git-content page commit))
    [:wb:comments]  (fn [x] (update-in x [:attrs :appkey] string/replace "$APPKEY$" APPKEY))))

(defn comment-content
  [page user]
  (en/at
    (en/html-resource (str THEME "/comment.html"))
    [:h2#title] (en/content ["Comments of " page])
    [:wb:comments]  (fn [x] (update-in x [:attrs :appkey] string/replace "$APPKEY$" APPKEY))
    [:script#wbcomment] (fn [x] (update-in x [:attrs :src] string/replace "$APPKEY$" APPKEY))))

(defn edit-content
  [page user]
  (en/at
    (en/html-resource (str THEME "/edit.html"))
    [:h2#title :> :span] (en/content page)
    [:form] (en/set-attr :action (page-url page))
    [:textarea#content] (en/content (try (slurp (page-file page)) 
                                      (catch FileNotFoundException e "")))))

(defn history-content
  [page user]
  (en/at
    (en/html-resource (str THEME "/history.html"))
    [:#history :tr.commit]
    (let [g (git DATA_DIR)
          commits (if page (git-log-flatten (g :log page :limit HISTORY_LIMIT) page)
                    (git-log-flatten (g :log :limit HISTORY_LIMIT)))]
      (en/clone-for [ci commits]
                    [[:td (en/attr= :name "date")]] (en/content (:date ci))
                    [[:td (en/attr= :name "author")]] (en/content (:author-name ci))
                    [[:a (en/attr= :name "page")]]
                    (comp
                      (en/content (:file ci))
                      (en/set-attr :href (page-url (:file ci))))
                    [[:a (en/attr= :name "view")]] 
                    (en/set-attr :href (page-url (:file ci) (:name ci)))))
    [:#history_limit] (en/content [(str HISTORY_LIMIT)])))

(defn attach-content
  [page user]
  (en/at
    (en/html-resource (str THEME "/attach.html"))
    [:#attachment :tr.attachment-list]
    (let [file-list (page-file-list page)]
      (en/clone-for [fl (into [] file-list)]
                    [[:td (en/attr= :name "name")]] (en/content (:name fl))
                    [[:td (en/attr= :name "size")]] (en/content (:size fl))
                    [[:td (en/attr= :name "time")]] (en/content (:time fl))
                    [[:a (en/attr= :name "link")]] (en/set-attr :href (:path fl) 
                                                                    :target "_blank")))   
    [:form] (en/set-attr :action (attach-url page))))

;; the pages
(en/deftemplate view'
  (en/html-resource (str THEME "/template.html"))
  [page & {commit :commit user :user}]
  [:title] (en/content [PROJECT " > " page])
  [:a.home_url] (en/set-attr :href (page-url DEFAULT_PAGE))
  [:a.global_history_url] (en/set-attr :href (history-url))
  [:span.username] (en/content (if user ["| Logged in as " user]))

  ;; the menu
  [:li.nav_page] (if commit identity (en/add-class "active"))
  [:li.nav_commit] (if commit (comp (en/add-class "active") 
                                    (en/remove-attr :hidden)) nil)
  [:li.nav_edit] (if commit nil identity)
  [:li.nav_comment] (if commit nil identity)
  [:li.nav_attach] (if commit nil identity)

  [:li.nav_page :> :a] (comp (en/content page) (en/set-attr :href (page-url page)))
  [:li.nav_commit :> :a] (en/content ["[ " page (if commit (str " | " commit)) " ]"])
  [:li.nav_edit :> :a] (en/set-attr :href (edit-url page))
  [:li.nav_comment :> :a] (en/set-attr :href (comment-url page))
  [:li.nav_attach :> :a] (en/set-attr :href (attach-url page))
  [:li.nav_history :> :a] (en/set-attr :href (history-url page))
  ;; menu end

  [:#last_modified :> :span] (en/content 
                          (let [g (git DATA_DIR)
                                ci (first (filter #(if commit (= (:name %) commit)
                                                     true) (g :log page)))
                                date (:date ci)]
                            date))

  [:div#main] (en/content (view-content page commit user)))

(defn view
  [page & more]
  (if (.exists (io/file (page-file page)))
    (apply (partial view' page) more)
    (resp/redirect (edit-url page))))

(en/deftemplate comments
  (en/html-resource (str THEME "/template.html"))
  [page & {user :user}]
  [:html] (en/set-attr :xmlns:wb "http://open.weibo.com/wb")
  ;;[:head] (en/append (en/html [:script {:src (string/replace "http://tjs.sjs.sinajs.cn/open/api/js/wb.js?appkey=$APPKEY$" "$APPKEY$" APPKEY)}]))
  [:title] (en/content [PROJECT " > Comments of " page])
  [:a.home_url] (en/set-attr :href (page-url DEFAULT_PAGE))
  [:a.global_history_url] (en/set-attr :href (history-url))
  [:span.username] (en/content (if user ["| Logged in as " user]))

  ;; the menu
  [:li.nav_edit] nil
  [:li.nav_comment] (en/add-class "active")
  [:li.nav_attach] nil

  [:li.nav_page :> :a] (comp (en/content page) (en/set-attr :href (page-url page)))
  [:li.nav_comment :> :a] (en/content ["[ Comment | " page " ]"])
  [:li.nav_history :> :a] (en/set-attr :href (history-url page))
  ;; menu end

  [:p#last_modified] nil

  [:div#main] (en/content (comment-content page user)))

(en/deftemplate edit
  (en/html-resource (str THEME "/template.html"))
  [page & {user :user}]
  [:title] (en/content [PROJECT " > Editing " page])
  [:a.home_url] (en/set-attr :href (page-url DEFAULT_PAGE))
  [:a.global_history_url] (en/set-attr :href (history-url))
  [:span.username] (en/content (if user ["| Logged in as " user]))

  ;; the menu
  [:li.nav_edit] (en/add-class "active")

  [:li.nav_page :> :a] (comp (en/content page) (en/set-attr :href (page-url page)))
  [:li.nav_edit :> :a] (en/content ["[ Edit | " page " ]"])
  [:li.nav_comment :> :a] (en/set-attr :href (comment-url page))
  [:li.nav_attach :> :a] (en/set-attr :href (attach-url page))
  [:li.nav_history :> :a] (en/set-attr :href (history-url page))
  ;; menu end
  
  [:p#last_modified] nil

  [:div#main] (en/content (edit-content page user)))

(en/deftemplate history
  (en/html-resource (str THEME "/template.html"))
  [page & {user :user}]
  [:title] (en/content [PROJECT " > History of " (or page "*")])
  [:a.home_url] (en/set-attr :href (page-url DEFAULT_PAGE))
  [:a.global_history_url] (en/set-attr :href (history-url))
  [:span.username] (en/content (if user ["| Logged in as " user]))

  ;; the menu
  [:li.nav_history] (en/add-class "active")

  [:li.nav_page :> :a] (comp (en/content page) (en/set-attr :href (page-url page)))
  [:li.nav_edit :> :a] (en/set-attr :href (edit-url page))
  [:li.nav_comment :> :a] (en/set-attr :href (comment-url page))
  [:li.nav_attach :> :a] (en/set-attr :href (attach-url page))
  [:li.nav_history :> :a] (en/content ["[ History | " page " ]"])
  ;; menu end

  [:p#last_modified] (en/content nil)

  [:div#main] (en/content (history-content page user)))

(en/deftemplate attach'
  (en/html-resource (str THEME "/template.html"))
  [page & {user :user}]
  [:title] (en/content [PROJECT " > Attachments of " page])
  [:a.home_url] (en/set-attr :href (page-url DEFAULT_PAGE))
  [:a.global_history_url] (en/set-attr :href (history-url))
  [:span.username] (en/content (if user ["| Logged in as " user]))

  ;; the menu
  [:li.nav_attach] (en/add-class "active")

  [:li.nav_page :> :a] (comp (en/content page) (en/set-attr :href (page-url page)))
  [:li.nav_edit :> :a] (en/set-attr :href (edit-url page))
  [:li.nav_comment :> :a] (en/set-attr :href (comment-url page))
  [:li.nav_attach :> :a] (en/content ["[ Attach | " page " ]"])
  [:li.nav_history :> :a] (en/set-attr :href (history-url page))
  ;; menu end

  [:p#last_modified] (en/content nil)

  [:div#main] (en/content (attach-content page user)))

(defn attach
  [page & more]
  (if (.exists (io/file (page-file page)))
    (apply (partial attach' page) more)
    (resp/redirect (edit-url page))))

;; the action
(defn save
  [req page]
  (let [{user :basic-authentication
         {input "data"} :form-params} req
        g (git DATA_DIR)]
    (with-open [w (io/writer (page-file page))] ;TODO handle exception e.g. no permission to create DATA_DIR
      (.write w input))
    (g :add page)
    (g :commit user)
    (g :gc))
  (resp/redirect (page-url page)))

(defn upload
  [page req]
  ;; for debugging
  #_(str "<h1> request-data:" page " : "  req  "</h1>")
  ;; store file
  (let [{{comm :comment 
          {upfile :tempfile filename :filename} :file }:params} req
        filep (upload-file page)
        filen (upload-file page filename)]
    (if (not (.exists (io/file filep)))
      (.mkdirs (File. filep)))
    (io/copy (io/file upfile) 
             (io/file filen)))
  (resp/redirect (attach-url page)))

;; the handlers
(defroutes public-handler
  ;; for debugging
  #_(GET "*" req {:status 200 :body (with-out-str (pprint req))})
  ;; view
  (GET "/" req (view DEFAULT_PAGE :user (auth/user req)))
  (GET "/page/:page/:commit" [page commit :as req] 
       (view page :commit commit :user (auth/user req)))
  (GET "/page/:page" [page :as req] (view page :user (auth/user req)))
  ;; comment
  (GET "/comment/:page" [page :as req] (comments page :user (auth/user req)))
  ;; attachments
  (GET "/files/:page/:file" 
       [page file :as req] 
       (let [f (File. (str "./files/" page "/" file))]
         {:status 200 
          :header {"Content-Disposition" (str "attachment;filename=" file)} 
          :body (if (.exists f) f "File not exists")
          }))
  ;; static resources
  (compojure.route/resources "/" {:root THEME}))

(defroutes protected-handler
  ;; history
  (GET "/history" req (history nil :user (auth/user req)))
  (GET "/history/:page" [page :as req] (history page :user (auth/user req)))
  ;; save
  (POST "/page/:page" [page :as req] (save req page))
  ;; edit
  (GET "/edit/:page" [page :as req] (edit page :user (auth/user req)))
  ;; attach
  (GET "/attach/:page" [page :as req] (attach page :user (auth/user req)))
  (wrap-multipart-params (POST "/attach/:page" [page :as req] (upload page req))))

(defroutes last-handler
  ;; 404
  (compojure.route/not-found "Not Found"))

(defroutes handler
  public-handler
  (wrap-basic-authentication protected-handler authenticated?)
  last-handler) ;; in fact all handler after wrap-basic-authentication will be protected...

;; the web app
(def app
  (-> handler
      (compojure.handler/site)))
