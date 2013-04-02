(ns gitwiki.git
  (:gen-class)
  (:import [java.util Date]
           [org.eclipse.jgit.storage.file FileRepository]
           [org.eclipse.jgit.api Git])
  (:require [clojure.java.io :as io]))

;; http://download.eclipse.org/jgit/docs/latest/apidocs/

(defn clone
  [remote path]
  (-> Git .cloneRepository (.setURI remote) (.setDirectory (io/file path)) .call))

(defn init
  [repo]
  (.create repo))

(defn repo
  [path]
  (let [r (str path "/.git")
        repo (FileRepository. r)]
    (or (.exists (io/file r)) (init repo))
    repo))

(defn git
  [repo]
  (org.eclipse.jgit.api.Git. repo))

(defn gc
  [repo]
  (-> git .gc .call))

(defn add
  [git & file-pattern]
  (-> git .add (.addFilepattern (or file-pattern ".")) .call))

(defn rm
  [git file-pattern]
  (-> git .rm (.addFilepattern file-pattern) .call))

(defn commit
  [git & [message author]]
  (-> git .commit 
      (.setMessage (or message (str "committed at " (Date.))))
      (.setAuthor (or author "unknown") "no email") 
      .call))

(defn push
  [git]
  (-> git .push .call))

(defn pull
  [git]
  (-> git .pull .call))

