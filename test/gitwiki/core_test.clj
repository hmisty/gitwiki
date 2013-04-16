(ns gitwiki.core_test
  (:use [clojure.test])
  (:require [gitwiki.core :as core]))

(deftest test-core
  "Return true or false of the given []"
  []
  (is (core/authenticated? "test" "test"))
  (are [x y] (.equals x y)
       "/page/test" (core/page-url "test")
       "/page/test/test" (core/page-url "test" "test")
       "/edit/test" (core/edit-url "test")
       "/history" (core/history-url)
       "/history/test" (core/history-url "test")
       "data/test" (core/page-file "test")
       "<p>test</p>" (core/parse "test")))
