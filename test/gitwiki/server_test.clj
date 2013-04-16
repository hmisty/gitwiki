(ns gitwiki.server_test
  (:use [clojure.test])
  (:require [gitwiki.server :as server]))

(deftest test-server
  "Return true or fase of the given []"
  []
  (is (fn? server/-main)))
