(ns gitwiki.git
  (:gen-class)
  (:import [java.util Date]
           [org.eclipse.jgit.storage.file FileRepository]
           [org.eclipse.jgit.treewalk TreeWalk CanonicalTreeParser EmptyTreeIterator]
           [org.eclipse.jgit.revwalk RevWalk RevCommit]
           [org.eclipse.jgit.diff DiffFormatter]
           [org.eclipse.jgit.util.io DisabledOutputStream]
           [org.eclipse.jgit.api Git])
  (:require [clojure.java.io :as io]))

;; References
;; http://download.eclipse.org/jgit/docs/latest/apidocs/

;; Terms
;; commit-name = rev-spec = commit-id = e.g. 576a36245f6d645c984a1ed3bbed4c4633c56746

(defn- clone
  [remote path]
  (-> (Git/cloneRepository) (.setURI remote) (.setDirectory (io/file path)) .call))

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

(defn- commit-date
  "Returns the formatted date time of the commit."
  [commit]
  (.format (java.text.SimpleDateFormat. "E MMM d HH:mm:ss yyyy Z")
           (java.util.Date. (* 1000 (-> commit .getCommitTime)))))

(defn- name-status
  "Returns the diff --name-status between current rev and its parent."
  [git commit]
  (let [r  (.getRepository git)
        revwalk (RevWalk. r)
        df (DiffFormatter. DisabledOutputStream/INSTANCE)
        _  (.setRepository df r)
        parent-count (.getParentCount commit)
        empty-tree-itor (EmptyTreeIterator.)
        commit-tree (.getTree commit)
        commit-tree-itor (CanonicalTreeParser. nil (.getObjectReader revwalk) (.getTree commit))]
    (map #(vector (.getNewPath %) (.. % getChangeType toString))
         (seq (if 
           (= parent-count 0) (.scan df empty-tree-itor commit-tree-itor)
           (.scan df (.. commit (getParent 0) getTree) commit-tree))))))

(defn- expand-rev-commit
  "Expand the information of a list of RevCommit:s."
  [git commits]
  (map (fn [ci]
         {:name (.name ci)
          :author [(.. ci getAuthorIdent getName) (.. ci getAuthorIdent getEmailAddress)]
          :date (commit-date ci)
          :message (.getFullMessage ci)
          :name-status (name-status git ci)}) commits))

(defn- log'
  ([git] (seq (-> git .log .call)))
  ([git path] (seq (-> git .log (.addPath path) .call))))

(defn- log 
  ([git] (expand-rev-commit git (log' git)))
  ([git path] (expand-rev-commit git (log' git path))))

(defn- cat-file
  ([git path] ;; read the last committed version
   (let [r (.getRepository git)
         commits (log' git path)
         c (first commits)
         tree (-> c .getTree)
         treewalk (TreeWalk/forPath r path tree)
         obj (-> treewalk (.getObjectId 0))
         in (-> r (.open obj) .openStream)]
     (slurp in)))
  ([git path commit-name] ;; read the specified committed version
   (let [r (.getRepository git)
         revwalk (RevWalk. r)
         c (.parseCommit revwalk (.resolve r commit-name)) ; here
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

