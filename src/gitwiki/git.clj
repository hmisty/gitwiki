(ns gitwiki.git
  (:gen-class)
  (:import [java.util Date]
           [org.eclipse.jgit.storage.file FileRepository]
           [org.eclipse.jgit.treewalk TreeWalk]
           [org.eclipse.jgit.revwalk RevWalk RevCommit]
           [org.eclipse.jgit.api Git])
  (:require [clojure.java.io :as io]))

;; References
;; http://download.eclipse.org/jgit/docs/latest/apidocs/

(defn- clone
  [remote path]
  (-> Git .cloneRepository (.setURI remote) (.setDirectory (io/file path)) .call))

(defn- init
  [repo]
  (.create repo))

(defn- gc
  [git]
  (-> git .gc .call))

(defn- add
  [git & [file-pattern]]
  (-> git .add (.addFilepattern (or file-pattern ".")) .call))

(defn- rm
  [git file-pattern]
  (-> git .rm (.addFilepattern file-pattern) .call))

(defn- commit
  [git & [author message]]
  (-> git .commit 
      (.setAuthor (or author "unknown") "no email") 
      (.setMessage (or message (str "committed at " (Date.))))
      .call))

(defn- log
  ([git] (seq (-> git .log .call)))
  ([git path] (seq (-> git .log (.addPath path) .call))))

(defn- cat-file
  ([git path] ;; read the last committed version
   (let [r (.getRepository git)
         commits (log git path)
         c (first commits)
         tree (-> c .getTree)
         treewalk (TreeWalk/forPath r path tree)
         obj (-> treewalk (.getObjectId 0))
         in (-> r (.open obj) .openStream)]
     (slurp in)))
  ([git path rev-spec] ;; read the specified committed version
   (let [r (.getRepository git)
         revwalk (RevWalk. r)
         c (.parseCommit revwalk (.resolve r rev-spec)) ; here
         tree (-> c .getTree)
         treewalk (TreeWalk/forPath r path tree)
         obj (-> treewalk (.getObjectId 0))
         in (-> r (.open obj) .openStream)]
     (slurp in))))

(defn- push
  [git]
  (-> git .push .call))

(defn- pull
  [git]
  (-> git .pull .call))

(defn- repo
  [path]
  (let [r (str path "/.git")
        repo (FileRepository. r)]
    (or (.exists (io/file r)) (init repo))
    repo))

(defn- git'
  [repo]
  (org.eclipse.jgit.api.Git. repo))

(defn git
  [path]
  (let [r (repo path)
        g (git' r)]
    (fn [cmd & args]
      (case cmd
        :clone         (apply clone args)
        :init          (init r)
        :gc            (gc g)
        :add           (apply (partial add g) args)
        :rm            (apply (partial rm g) args)
        :commit        (apply (partial commit g) args)
        :log           (apply (partial log g) args)
        :cat-file      (apply (partial cat-file g) args)
        :push          (push g)
        :pull          (pull g)))))

