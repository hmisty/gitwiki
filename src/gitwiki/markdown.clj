(ns gitwiki.markdown
  (:gen-class)
  (:use markdown.core))

(def parse
  "Returns HTML of the given markdown texts."
  md-to-html-string)
