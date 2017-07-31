(defproject slackers-and-slayers "0.1.0-SNAPSHOT"
  :description "Little chatbot for Slack."
  :url "https://github.com/heptahedron/slackers-and-slayers"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [aleph "0.4.3"]
                 [org.clojure/data.json "0.2.6"]]
  :main ^:skip-aot slackers-and-slayers.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
