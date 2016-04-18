#!/usr/bin/env boot

(set-env!
 :source-paths #{"src"}
 :resource-paths #{"resources"}
 :target-path "target"
 :repositories #(conj % '["confluent" {:url "http://packages.confluent.io/maven"}])

 :dependencies '[
   [org.clojure/clojure "1.8.0"]
   [ymilky/franzy "0.0.2-SNAPSHOT"]
   [io.confluent/kafka-avro-serializer "2.0.1"]
   [com.fasterxml.jackson.core/jackson-core "2.7.3"]
   [com.fasterxml.jackson.core/jackson-databind "2.7.3"]
   [aleph "0.4.1-beta7"]
   [byte-streams "0.2.0"]
   [com.damballa/abracad "0.4.13"]
   [pjson "0.3.2"]
   [com.taoensso/nippy "2.11.1"]
   [environ "1.0.2"]
   [manifold "0.1.4"]
   [boot-environ "1.0.2"]
   [com.taoensso/nippy "2.11.1"]
   [org.danielsz/system "0.3.0-SNAPSHOT"]
   [com.stuartsierra/component "0.3.1"]
   [prismatic/schema "1.1.0"]
   [org.clojure/core.async "0.2.374"]])

(task-options!
 pom {:project 'foxcomm_messaging
      :version "0.1.0"}

 aot {:namespace #{'messaging.main}}
    ; aot {:all true}
 jar {:main 'messaging.main
      :file "messaging.jar"}
 target {:dir #{"target"}})

(set! *warn-on-reflection* true)

(deftask build
 "Build project and make uberjar"
 []
 (comp
  (aot)
  (pom)
  (uber)
  (jar)
  (sift :include #{#"messaging.jar"})
  (target)))
