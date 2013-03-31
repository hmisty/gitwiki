;; package to a standalone jar
;; run as a standalone server

(ns gitwiki.server 
  (:gen-class)
  (:use [gitwiki.core :only (app)])
  (:require [ring.adapter.jetty :as jetty]))

(defn -main [& [port]]
  (let [port (if port (Integer/parseInt port) 8083)]
    (jetty/run-jetty #'app {:join? false :port port})))
