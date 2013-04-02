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

(defn repo
  [path]
  (let [r (str path "/.git")
        repo (FileRepository. r)]
    (or (.exists (io/file r)) (.create repo))
    repo))

(defn create
  [repo]
  (.create repo))

(defn git
  [repo]
  (org.eclipse.jgit.api.Git. repo))

(defn add
  [git & file-pattern]
  (-> git .add (.addFilepattern (or file-pattern ".")) .call))

(defn commit
  [git & message]
  (-> git .commit (.setMessage (or message (str "committed at " (Date.)))) .call))

(defn push
  [git]
  (-> git .push .call))

(defn pull
  [git]
  (-> git .pull .call))

