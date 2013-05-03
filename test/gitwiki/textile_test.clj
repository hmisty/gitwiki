(ns gitwiki.textile_test
  (:use [clojure.test])
  (:require [gitwiki.textile :as textile]))

(deftest test-textile
  "Return true or false of the given []"
  []
  (is (.equals (textile/parse "test") "<p>test</p>")))
