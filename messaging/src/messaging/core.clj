(ns messaging.core
 (:require
   ;; std
   [clojure.core.async
    :as async
    :refer [<!! chan thread go]]
   [clojure.java.io :as io]
   [clojure.string :as string]
   ;; log
   [taoensso.timbre :as log]
   ;; internal
   [messaging.mail :as mail]
   ;; kafka & other libs
   [franzy.clients.consumer.client :as consumer]
   [franzy.clients.consumer.protocols :refer :all]
   [cheshire.core :as json]
   [environ.core :refer [env]]))


(def topics ["scoped_activities"])

(def kafka-broker (delay (:kafka-broker env)))
(def schema-registry-url (delay (:schema-registry-url env)))


(defn decode-embed-json
  [^String s]
  (some-> s
          (string/replace #"\\" "")
          json/parse-string))

(defn decode-activity-json
 "return same Activity map but with parsed :data and :context from json"
 [msg]
 (as-> msg $
     (clojure.walk/keywordize-keys $)
     (assoc $
       :data (decode-embed-json (:data $))
       :context (decode-embed-json (:context $)))))

(defn decode
  [message]
  (-> message
      :value
      str
      json/parse-string
      decode-activity-json))


(defn- safe-decode-minimal
  [message]
  (try
    (let [msg (-> message :value str)]
      (try
        (json/parse-string msg)
        (catch Exception _ msg)))
    (catch Exception _
      message)))


(def stop (atom false))

(defn start-app
  [react-app]
  (log/infof "Start consumer, with kafka=%s schema=%s"
             @kafka-broker
             @schema-registry-url)
  (reset! stop false)
  (let [cc {:bootstrap.servers       [@kafka-broker]
            :group.id                "fc-messaging"
            :auto.offset.reset       :latest
            :key.deserializer        "org.apache.kafka.common.serialization.ByteArrayDeserializer"
            :schema.registry.url     @schema-registry-url
            :value.deserializer      "io.confluent.kafka.serializers.KafkaAvroDeserializer"
            :enable.auto.commit      false}]
    (with-open [c (consumer/make-consumer cc)]
      (subscribe-to-partitions! c topics)
      (log/info "Partitions subscribed to:" (partition-subscriptions c))
      (loop []
        (let [cr (poll! c)]
          (doseq [record cr]
            (try
              (let [msg (decode record)]
                (log/debug msg)
                (mail/handle-activity msg)
                (commit-offsets-sync! c {(select-keys record [:topic :partition])
                                        {:offset (:offset record) :metadata ""}}))
              (catch Exception e
                (log/errorf "Caught exception: %s\n\tDuring processing %s" e
                            (safe-decode-minimal record))))))

       (when-not @stop
        (recur))))
    (log/info "stop polling kafka activities")))

(defn stop-app []
  (reset! stop true))
