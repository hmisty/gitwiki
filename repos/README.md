# Local Maven Repository

Here stored the needed jar libraries which are not in the central maven repository.

For this project, here we have:

* wikitext-core : org.eclipse.mylyn.wikitext.core 1.8.0.I20130327-2301
* wikitext-textile-core : org.eclipse.mylyn.wikitext.textile.core 1.8.0.I20130327-2301

because the older versions of wikitext.textile has a bug
for parsing

	bc.. sample

	some codes

	p. end of sample

incorrectly to

	<pre><pre>...</pre></pre>

rather than the expected one

	<pre><code>...</code></pre>

## Use

Add to project.clj:

	:dependencies [ ...
		[local/wikitext-core "1.8.0"]
		[local/wikitext-textile-core "1.8.0"]
		... ]
	:repositories {"local-repos" "file:repos"}

Please do put :repositories after :dependencies otherwise lein will not search the online repositories at all.

## Setup

For deploying jars to the local repository, the following maven command can be used:

	mvn deploy:deploy-file -DgroupId=local -DartifactId={XXX} -Dversion={X.Y.Z} -Dpackaging=jar -Dfile={/path/to/jar} -Durl=file:repos

For example,

	mvn deploy:deploy-file -DgroupId=local -DartifactId=wikitext-core -Dversion=1.8.0 -Dpackaging=jar -Dfile=/Users/liu/Downloads/wikitext-standalone-1.8.0-SNAPSHOT/org.eclipse.mylyn.wikitext.core_1.8.0.I20130327-2301.jar -Durl=file:repos
	mvn deploy:deploy-file -DgroupId=local -DartifactId=wikitext-textile-core -Dversion=1.8.0 -Dpackaging=jar -Dfile=/Users/liu/Downloads/wikitext-standalone-1.8.0-SNAPSHOT/org.eclipse.mylyn.wikitext.textile.core_1.8.0.I20130327-2301.jar -Durl=file:repos

## Important

DO NOT ignoring ```*jar``` in your .gitignore!

