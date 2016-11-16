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
                 ;; http stack
                 [aleph "0.4.2-alpha8"]
                 [compojure "1.5.1"]
                 [ring/ring-core "1.5.0"]
                 ;; email
                 [gws/clj-mandrill "0.4.2"]
                 [org.clojars.narma/clojchimp "1.0.2"]
                 [byte-streams "0.2.0"]
                 [com.damballa/abracad "0.4.13"]
                 [pjson "0.3.2"]
                 [com.taoensso/nippy "2.11.1"]
                 [environ "1.0.2"]
                 [manifold "0.1.5"]
                 [boot-environ "1.0.2"]
                 [com.taoensso/nippy "2.11.1"]
                 [org.danielsz/system "0.3.0-SNAPSHOT"]
                 [com.stuartsierra/component "0.3.1"]
                 [prismatic/schema "1.1.0"]
                 [org.clojure/core.async "0.2.374"]])

(task-options!
  pom {:project 'foxcomm_messaging
       :version "0.1.0"}

  aot {:all true}
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

(defn- generate-lein-project-file! [& {:keys [keep-project] :or {keep-project true}}]
  (require 'clojure.java.io)
  (let [pfile ((resolve 'clojure.java.io/file) "project.clj")
        ; Only works when pom options are set using task-options!
        {:keys [project version]} (:task-options (meta #'boot.task.built-in/pom))
        prop #(when-let [x (get-env %2)] [%1 x])
        head (list* 'defproject (or project 'boot-project) (or version "0.0.0-SNAPSHOT")
               (concat
                 (prop :url :url)
                 (prop :license :license)
                 (prop :description :description)
                 [:dependencies (conj (get-env :dependencies)
                                      ['boot/core "2.6.0" :scope "compile"])
                  :repositories (get-env :repositories)
                  :source-paths (vec (concat (get-env :source-paths)
                                             (get-env :resource-paths)))]))
        proj (pp-str head)]
      (if-not keep-project (.deleteOnExit pfile))
      (spit pfile proj)))

(deftask lein-generate
  "Generate a leiningen `project.clj` file.
   This task generates a leiningen `project.clj` file based on the boot
   environment configuration, including project name and version (generated
   if not present), dependencies, and source paths. Additional keys may be added
   to the generated `project.clj` file by specifying a `:lein` key in the boot
   environment whose value is a map of keys-value pairs to add to `project.clj`."
 []
 (generate-lein-project-file! :keep-project true))
