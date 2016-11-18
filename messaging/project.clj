(defproject
  foxcomm_messaging
  "0.1.0"
  :plugins [[lein-environ "1.1.0"]]

  :profiles {:uberjar {:aot :all}
             :dev
             {:source-paths ["dev"]
              :repl-options {:init-ns user
                             :init (require '[clojure.tools.namespace.repl :refer [refresh]])
                             :welcome (println "Use (refresh) to reload code.")}
              :dependencies [[org.clojure/tools.namespace "0.2.11"]
                             [com.taoensso/nippy "2.11.1"]]

              :env {:phoenix-password "password"
                     :phoenix-user "admin@admin.com"
                     :phoenix-url "https://admin.foxcommerce.local/api"
                     :api-host "127.0.0.1"
                     :kafka-broker "127.0.0.1:9092"
                     :schema-registry-url "http://127.0.0.1:8081"}}}

  :dependencies
  [[org.clojure/clojure "1.8.0"]
   [ymilky/franzy "0.0.2-20160325.161319-2"]
   [io.confluent/kafka-avro-serializer "2.0.1"]
   [com.fasterxml.jackson.core/jackson-core "2.7.3"]
   [com.fasterxml.jackson.core/jackson-databind "2.7.3"]
   [aleph "0.4.2-alpha8"]
   [com.taoensso/timbre "4.7.4"]
   [compojure "1.5.1"]
   [ring/ring-core "1.5.0"]
   [gws/clj-mandrill "0.4.2"]
   [org.clojars.narma/clojchimp "1.0.2"]
   [byte-streams "0.2.0"]
   [com.damballa/abracad "0.4.13"]
   [pjson "0.3.2"]
   [environ "1.0.2"]
   [manifold "0.1.5"]
   [org.danielsz/system "0.3.0-20160513.104026-20"]
   [com.stuartsierra/component "0.3.1"]
   [prismatic/schema "1.1.0"]
   [org.clojure/core.async "0.2.374"]]
  :repositories
  [["clojars" {:url "https://clojars.org/repo/"}]
   ["maven-central" {:url "https://repo1.maven.org/maven2"}]
   ["confluent" {:url "http://packages.confluent.io/maven"}]]
  :source-paths
  ["src" "resources"]
  :jar-name "messaging-no-dependencies.jar"
  :uberjar-name "messaging.jar"
  :main messaging.main)
