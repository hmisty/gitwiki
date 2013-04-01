(defproject gitwiki "0.1.0-SNAPSHOT"
  :description "gitwiki: a simple, light and easy to use wiki with git backend"
  :url "http://github.com/hmisty/gitwiki"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [ring "1.0.1"]
                 [ring-basic-authentication "1.0.2"]
                 [compojure "1.0.1"]
                 [enlive "1.0.0"]
                 [org.eclipse.mylyn.wikitext/wikitext.textile "0.9.4.I20090220-1600-e3x"]]
  :main gitwiki.server 
  :plugins [[lein-ring "0.7.5"]]
  :ring {:handler gitwiki.core/app})
