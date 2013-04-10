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

;================================================================================================


(defrecord LineData [name passwd])

(defn get-line [line-str] 
	(->LineData 
		(let [line line-str] (.substring line 0 (.indexOf line ":" 0))) 
		(let [line line-str] (.substring line (+ (.indexOf line ":" 0) 1))) 
	))

(defn openF []
	(with-open [rdr (io/reader "src/gitwiki/gitwiki.us")] (doall (map #(get-line %) (line-seq rdr))))
)

;Apache_htpasswd_sha1_Java
;"{SHA}" + new sun.misc.BASE64Encoder().encode(java.security.MessageDigest.getInstance("SHA1").digest(password.getBytes()))

(defn get-hash [type data]
	(Base64/encodeBase64String (.digest (java.security.MessageDigest/getInstance type) (.getBytes data) )))	
(defn sha1 [data]
	(get-hash "SHA1" data))


(defn is-auth-user? [n p]
	(reduce #(or %1 %2) (map #(and (= (:name %) n) (= (:passwd %) (str "{SHA}" (sha1 p)))) (openF)))
)

	
	;
	;
	;======================================================
	;
	;ADD DEPS
	;
	;[commons-codec/commons-codec "1.7"]
	;
	;
	;*************************************************
	;
	;
	;
	;REPL TEST COMMANDS
	;
	;=================================================
	;
	;(take 1 (line-seq (reader "src/examples/gitwiki.us")))
	;
	;(with-open [rdr (reader "src/examples/gitwiki.us")] (count (line-seq rdr)))
	;
	;(with-open [rdr (reader "src/examples/gitwiki.us")] (doall (line-seq rdr)))
	;
	;(.indexOf "read-line-data" ":" 0)
	;
	;
	;
	;=================================================
	;
	;(let [line line-str] (.substring line 0 (.indexOf line ":" 0)))
	;
	;(let [line "aaa:bccc"] (.substring line (+ (.indexOf line ":" 0) 1)))
	;
	;=================================================
	;
	;(defrecord LineData [name passwd])
	;(defn get-line [line-str] 
	;	(->LineData 
	;		(let [line line-str] (.substring line 0 (.indexOf line ":" 0))) 
	;		(let [line line-str] (.substring line (+ (.indexOf line ":" 0) 1))) 
	;	))
	;
	;=================================================
	;
	;(defn openF []
	;	(with-open [rdr (reader "src/examples/gitwiki.us")] (doall (map #(get-line %) (line-seq rdr))))
	;)
	;=================================================
	;
	;
	;(defn my-or [m n] (or m n))
	;
	;
	;(defn is-auth-user? [n p]
	;	(reduce #(or %1 %2) (map #(and (= (:name %) n) (= (:passwd %) p)) (openF)))
	;)
	;
	;=================================================
	;
	;(filter is-auth-user? )
	;
	;=================================================
	;
	;
	;(select #(and (= (:name %) "aaa") (= (:passwd %) "bccc"))  {:name })
	;
	;
	;=================================================
	;
	;
	;(Base64/encodeBase64String (.digest (java.security.MessageDigest/getInstance "sha1") (.getBytes "123")))
	;
	;==================================================
	;
	;(defn md5
	; "basic : Generate a md5 checksum for the given string"
	; [token]
	;  (let [hash-bytes
	;		 (doto (java.security.MessageDigest/getInstance "MD5")
	;			   (.reset)
	;			   (.update (.getBytes token)))]
	;	   (.toString
	;		 (new java.math.BigInteger 1 (.digest hash-bytes)) 
	;		 ; Positive and the size of the number
	;		 16)))
	;




