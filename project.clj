(defproject cloanda "0.1.0-SNAPSHOT"
  :description "A clojure wrapper for Oanda REST API"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "1.0.1"]
                 [org.clojure/tools.logging "0.3.1"]
		]
  :main ^:skip-aot cloanda.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
