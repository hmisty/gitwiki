(ns gitwiki.auth
  {:author "Evan Liu (hmisty)"}
  (:gen-class)
  (:require [clojure.data.codec.base64 :as base64]
			[clojure.java.io :as io])
  (:import (java.security MessageDigest)
		 (java.io FileInputStream FileOutputStream)
		 (org.apache.commons.codec.binary Base64))
  )

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

(def line-data 
  "Return an atom Map with '' value"
  (atom {:name "" :passwd ""}))

(defn get-line
  "Return the parsed string line" 
  [line]
  (reset! line-data {:name (let [l line] (.substring l 0 (.indexOf l ":" 0))) 
		     :passwd (let [l line] (.substring l (+ (.indexOf l ":" 0) 1))) }))

(defn open-file 
  "Open the file this path and Return parsed each line data . path eg: src/auth/gitwiki.us"
  [path]
  (with-open [rdr (io/reader path)] (doall (map #(get-line %) (line-seq rdr)))))

(defn get-hash 
  "Return the Bash64-encoded hash data of some digest type"
  [type data]
  (Base64/encodeBase64String (.digest (java.security.MessageDigest/getInstance type) (.getBytes data) )))

(defn sha1 
  "Return sha1 digested data"
  [data]
  (get-hash "SHA1" data))

(defn is-auth-user? 
  "Return if the n(name) and p(password) is authenticated , f is the password file location"
  [n p f]
  (reduce #(or %1 %2) (map #(and (= (:name %) n) (= (:passwd %) (str "{SHA}" (sha1 p)))) (open-file f))))
