(defproject gitwiki "0.4.0"
  :description "gitwiki: a simple, light and easy to use wiki with git backend"
  :url "http://github.com/hmisty/gitwiki"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [ring "1.0.1"]
                 [ring-basic-authentication "1.0.2"]
                 [compojure "1.0.1"]
                 [enlive "1.1.1"]
                 [local/wikitext-core "1.8.0"]
                 [local/wikitext-textile-core "1.8.0"]
                 #_[org.eclipse.mylyn.wikitext/wikitext.textile "0.9.4.I20090220-1600-e3x"]
                 #_[org.fusesource.wikitext/textile-core "1.4"]
                 [org.eclipse.jgit/org.eclipse.jgit.pgm "2.3.1.201302201838-r"]
                 [markdown-clj "0.9.20"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :repositories {"local-repos" "file:repos"}
  :main gitwiki.server 
  :plugins [[lein-ring "0.7.5"]]
  :ring {:handler gitwiki.core/app})
