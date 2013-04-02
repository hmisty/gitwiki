(ns gitwiki.textile
  (:gen-class)
  (:import 
    [java.io StringWriter]
    [org.eclipse.mylyn.wikitext.core.parser MarkupParser]
    [org.eclipse.mylyn.wikitext.core.parser.builder HtmlDocumentBuilder]
    [org.eclipse.mylyn.wikitext.textile.core TextileLanguage]
    ))

;; References
;; http://blog.oio.de/2012/03/21/mylyn-wikitext-an-open-source-library-for-handling-wiki-markup/
;; http://help.eclipse.org/juno/topic/org.eclipse.mylyn.wikitext.help.ui/help/devguide/Using-The-WikiText-Parser.html?cp=44_3_1_1#AdvancedParserUsage

(defn parse
  "Parse textile and generate HTML."
  [textile]
  (let [markup (TextileLanguage.)
        parser (MarkupParser. markup)
        writer (StringWriter.)
        builder (HtmlDocumentBuilder. writer)]
    (.setEmitAsDocument builder false)
    (.setBuilder parser builder)
    (.parse parser textile)
    (.toString writer)))
