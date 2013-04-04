(ns gitwiki.auth
  {:author "Evan Liu (hmisty)"}
  (:gen-class)
  (:require [clojure.data.codec.base64 :as base64]))

(defn- byte-transform
  "Used to encode and decode strings."
  [direction-fn string]
  (reduce str (map char (direction-fn (.getBytes string)))))

(defn- encode-base64
  "Will do a base64 encoding of a string and return a string."
  [^String string]
  (byte-transform base64/encode string))

(defn- decode-base64
  "Will do a base64 decoding of a string and return a string."
  [^String string]
  (byte-transform base64/decode string))

(defn user
  "Returns the logged in username by extracting from the HTTP request header."
  [req]
  (let [auth ((:headers req) "authorization")
        cred (and auth
                  (decode-base64
                    (last
                      (re-find #"^Basic (.*)$" auth))))
        user (and cred
                  (last
                    (re-find #"^(.*):" cred)))]
    user))

