(defproject
  foxcomm_messaging
  "0.1.0"
  :dependencies
  [[org.clojure/clojure "1.8.0"]
   [ymilky/franzy "0.0.2-20160325.161319-2"]
   [io.confluent/kafka-avro-serializer "2.0.1"]
   [com.fasterxml.jackson.core/jackson-core "2.7.3"]
   [com.fasterxml.jackson.core/jackson-databind "2.7.3"]
   [clj-http "3.2.0"]
   [aleph "0.4.1-beta7"]
   [compojure "1.5.1"]
   [ring/ring-core "1.5.0"]
   [gws/clj-mandrill "0.4.2"]
   [org.clojars.narma/clojchimp "1.0.2"]
   [byte-streams "0.2.0"]
   [com.damballa/abracad "0.4.13"]
   [pjson "0.3.2"]
   [com.taoensso/nippy "2.11.1"]
   [environ "1.0.2"]
   [manifold "0.1.4"]
   [boot-environ "1.0.2"]
   [com.taoensso/nippy "2.11.1"]
   [org.danielsz/system "0.3.0-20160513.104026-20"]
   [com.stuartsierra/component "0.3.1"]
   [prismatic/schema "1.1.0"]
   [org.clojure/core.async "0.2.374"]
   [boot/core "2.6.0" :scope "compile"]]
  :repositories
  [["clojars" {:url "https://clojars.org/repo/"}]
   ["maven-central" {:url "https://repo1.maven.org/maven2"}]
   ["confluent" {:url "http://packages.confluent.io/maven"}]]
  :source-paths
  ["src" "resources"]
  :jar-name "messaging-no-dependencies.jar"
  :uberjar-name "messaging.jar"
  :main messaging.main
  :aot [messaging.main])
