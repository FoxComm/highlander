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
   [messaging.settings :as settings]
   ;; kafka & other libs
   [franzy.clients.consumer.client :as consumer]
   [franzy.clients.consumer.protocols :refer :all]
   [franzy.clients.consumer.defaults :as cd]
   [franzy.serialization.deserializers :as deserializers]
   [franzy.common.models.types :as mt]
   [cheshire.core :as json]
   [environ.core :refer [env]]
   [aleph.http :as http]
   [byte-streams :as bs]
   [franzy.clients.consumer.callbacks :as callbacks]))


(def topics ["activities"])

(def kafka-broker (delay (:kafka-broker env)))
(def schema-registry-url (delay (:schema-registry-url env)))


(defn decode-embed-json
  [^String s]
  (some-> s
          (string/replace #"\\" "")
          json/parse-string))

(defn transform-date
  [d]
  (let [year (get d :year)
        month (get d :month)
        day (get d :day)
        hour (get d :hour)
        minute (get d :minute)
        sec (get d :second)
        micro (get d :micro)]

   (-> (format "%04d-%02d-%02dT%02d:%02d:%02d.%03dZ" year month day hour minute sec micro)
       java.time.Instant/parse)))


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

(def stop (atom false))

(defn start-app
  [react-app]
  (reset! stop false)
  (let [cc {:bootstrap.servers       [@kafka-broker]
            :group.id                "fc-messaging"
            :auto.offset.reset       :earliest
            :key.deserializer        "org.apache.kafka.common.serialization.ByteArrayDeserializer"
            :schema.registry.url     @schema-registry-url
            :value.deserializer      "io.confluent.kafka.serializers.KafkaAvroDeserializer"
            :enable.auto.commit      false}]
    (with-open [c (consumer/make-consumer cc)]
      (subscribe-to-partitions! c topics)
      (log/info "Partitions subscribed to:" (partition-subscriptions c))
      (loop []
        (let [cr (poll! c)]
          (doseq [record cr :let [msg (decode record)]]
            (log/debug msg)
            (try
              (mail/handle-activity msg)
              (commit-offsets-async! c {(select-keys record [:topic :partition])
                                        {:offset (:offset record) :metadata ""}})
              (catch Exception e (log/error "Caught exception: " e)))))

       (when-not @stop
        (recur))))
    (log/info "stop polling kafka activities")))

(defn stop-app []
  (reset! stop true))
