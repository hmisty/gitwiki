(ns gitwiki.auth_test
  (:use [clojure.test])
  (:require [gitwiki.auth :as auth]))

(deftest test-auth
  "Return true or false of the given []"
  []
  (is (> (count (auth/read-users)) 0))
  (is (auth/is-valid-user? "test" "test"))
  (is (not (auth/is-valid-user? "liujiaqi" "123"))))
