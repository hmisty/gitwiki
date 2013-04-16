(ns gitwiki.git_test
  (:use [clojure.test])
  (:require [gitwiki.git :as git]))
(deftest test-git
  "Return true or false of the given []"
  []
  (is (fn? git/git)))
