(ns gitwiki.git
  (:gen-class)
  (:import [org.eclipse.jgit.storage.file FileRepository]
           [org.eclipse.jgit.api Git]))

;; http://download.eclipse.org/jgit/docs/latest/apidocs/

(defn repo
  [path]
  (let [r (FileRepository. (str path "/.git"))]
    (.create r) ;TODO handle if exists
    r))

(defn git
  [repo]
  (org.eclipse.jgit.api.Git. repo))


