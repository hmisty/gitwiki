gitwiki
=======

A simple, light and easy to use wiki with git backend

Quick Start
=======

	git clone https://github.com/hmisty/gitwiki.git
	cd gitwiki
	lein run

Open browser to http://127.0.0.1:8083

* A default testing user test with password test is built in for your start.

Theme
=======
It supports theme. The default one shipped in resources/default is borrowed from [wigit](https://github.com/remko/wigit).

You can absolutely compose your own flavored themes:

* view.html - the view page
* edit.html - the edit page
* history.html - the history page
* style.css - the style

Authentication
=======
HTTP Basic Authentication is employed.

To create or update user and password, use htpasswd:

	htpasswd -s gitwiki.user USERNAME

Under the Hood
=======
* programming language: [clojure][1]
* build tool: [leiningen][2]
* web stack:
	- templating: [enlive][3]
	- routing: [compojure][4]
	- http handling: [ring][5]
* wiki rendering: [textile][6]. org.eclipse.mylyn.wikitext Java library is reused and wrapped with clojure.
* git API: [jgit][7]. org.eclipse.jgit library is reused and wrapped with clojure for operating the git repository.

[1]: http://clojure.org/
[2]: http://leiningen.org/
[3]: https://github.com/cgrand/enlive
[4]: https://github.com/weavejester/compojure
[5]: https://github.com/ring-clojure/ring.git
[6]: http://en.wikipedia.org/wiki/Textile_(markup_language)
[7]: http://www.jgit.org

License
=======
Copyright © 2013 Evan Liu (hmisty)

Distributed under the Eclipse Public License, the same as Clojure.
