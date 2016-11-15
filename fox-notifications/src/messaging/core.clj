(ns messaging.core
 (:require
   ;; std
   [clojure.core.async
    :as async
    :refer [<!! chan thread go]]
   [clojure.java.io :as io]
   [clojure.string :as string]
   ;; internal
   [messaging.mail :as mail]
   [messaging.settings :as settings]
   ;; kafka & other libs
   [franzy.clients.consumer.client :as consumer]
   [franzy.clients.consumer.protocols :refer :all]
   [franzy.clients.consumer.defaults :as cd]
   [franzy.serialization.deserializers :as deserializers]
   [franzy.common.models.types :as mt]
   [pjson.core :as json]
   [environ.core :refer [env]]
   [aleph.http :as http]
   [byte-streams :as bs]
   [franzy.clients.consumer.callbacks :as callbacks])


 (:import [javax.script ScriptEngineManager]))


(def topics ["activities"])

(def kafka-broker (delay (:kafka-broker env)))
(def schema-registry-url (delay (:schema-registry-url env)))


(defn decode-embed-json
  [^String s]
  (some-> s
          (string/replace #"\\" "")
          json/read-str))

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
      json/read-str
      decode-activity-json))

(def stop (atom false))

(defn render-react-activity
  [react-app activity]
  (let [props (assoc activity
                :isRead false
                :kind (:activity_type activity))
        html (try
               (.eval react-app (str "renderNotificationItem(" (json/write-str props) ")"))
               (catch Exception e (println "Can't render activity" activity "\n" e)))
        text (some->> html
                      (re-seq #"(<div class=\"fc-activity-notification-item__text\".+?</div>)")
                      ffirst)]

    (some->>
      (some-> text
          (string/replace #"(<(?!(a\s|/a)).+?>)" " ")
          (string/replace #"<a\s.+?href=\"(.*?)\".+?>(.+?)</a>" (str "<" (settings/get :admin_base_url) "$1|$2>"))
          (string/replace #"\p{Zs}" " ")
          (string/split #"\s"))
      (map string/trim)
      (remove empty?)
      (string/join " "))))


(defn send-to-slack
  [^String msg]
  (http/post (settings/get :slack_webhook_url)
             {:body (json/write-str {:text msg})
              :content-type "application/json"
              :accept ["application/json"]}))

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
      (println "Partitions subscribed to:" (partition-subscriptions c))
      (loop []
        (let [cr (poll! c)]
          (doseq [record cr :let [msg (decode record)]]
            (prn msg)
            (commit-offsets-async! c {(select-keys record [:topic :partition])
                                      {:offset (:offset record) :metadata ""}})
            (try
              (mail/handle-activity msg)
              ; (commit-offsets-async! c {(select-keys record [:topic :partition])
              ;                           {:offset (:offset record) :metadata ""}})
              (catch Exception e (println "Caught exception: " e)))))

;; temporary disabled
;;               (when-let [string-msg (render-react-activity react-app msg)]
;;                 (send-to-slack string-msg))))))

       (when-not @stop
        (recur))))
    (println "exit")))

(defn stop-app []
  (reset! stop true))

(defn start-react-app
  []
  (let [nashorn (.getEngineByName (ScriptEngineManager.) "nashorn")]
    (.eval nashorn (slurp (io/resource "polyfill.js")))
    (.eval nashorn (slurp (io/resource "admin-dbg.js")))
    nashorn))
