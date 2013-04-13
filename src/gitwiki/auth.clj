(ns gitwiki.auth
  {:author "Evan Liu (hmisty)"}
  (:gen-class)
  (:require [clojure.data.codec.base64 :as base64]
            [clojure.string :as string]
            [clojure.java.io :as io]))

(def DEFAULT_FILE "gitwiki.user")

(defn- byte-transform
  "Returns the transformed bytes of the given bytes data."
  [direction-fn data]
  (reduce str (map char (direction-fn data))))

(defn- base64-encode-bytes
  "Returns the base-64 encoded string of the given bytes data."
  [data]
  (byte-transform base64/encode data))

(defn- base64-decode-bytes
  "Returns the base-64 decoded string of the given bytes data."
  [data]
  (byte-transform base64/decode data))

(defn- base64-encode
  [s]
  (base64-encode-bytes (.getBytes s)))

(defn- base64-decode
  [s]
  (base64-decode-bytes (.getBytes s)))

(defn user
  "Returns the logged in username by extracting from the HTTP request header."
  [req]
  (let [auth ((:headers req) "authorization")
        cred (and auth
                  (base64-decode
                    (last
                      (re-find #"^Basic (.*)$" auth))))
        user (and cred
                  (last
                    (re-find #"^(.*):" cred)))]
    user))

(defn read-users
  "Returns a map of user password read from the given file or default file gitwiki.user."
  ([] (read-users DEFAULT_FILE))
  ([file] (reduce #(apply (partial assoc %) (string/split %2 #":")) {} 
                          (string/split-lines (slurp file)))))

(defn sha1 
  "Returns sha1 of the given string s."
  [s]
  (.digest (java.security.MessageDigest/getInstance "SHA1") (.getBytes s)))

(defn is-valid-user? 
  "Returns true if the user and password is correct."
  [user password]
  (let [users (read-users)]
    (= (users user) (str "{SHA}" (base64-encode-bytes (sha1 password))))))

