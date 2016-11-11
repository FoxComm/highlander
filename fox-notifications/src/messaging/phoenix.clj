(ns messaging.phoenix
  (:require [aleph.http :as http]
            [clj-http.client :as hclient]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [messaging.settings :as settings]
            [ring.util.response :refer [response]]
            [utils.ring-json :refer [wrap-json-response wrap-json-body]]
            [pjson.core :as json]
            [byte-streams :as bs]
            [environ.core :refer [env]]))

(defn parse-int [s]
  (if (integer? s)
    s
    (Integer/parseInt s)))

(def api-port (delay (parse-int
                       (or
                         (:api-port env)
                         15054))))

(def api-host (delay (:api-host env)))
(def phoenix-email (delay (:phoenix-email env)))
(def phoenix-password (delay (:phoenix-password env)))


(def http-pool (delay (http/connection-pool
                        {:connection-options {:insecure? true}})))


(def plugin-info
  (delay {:name "fox-notifications"
          :description "Sends mail, slack notifications on events to customers/storeAdmins"
          :apiHost @api-host
          :version "1.0"
          :apiPort @api-port
          :schemaSettings settings/schema}))


(def api-server (delay (:api-server env)))


(defroutes app
  (GET "/_settings/schema" [] (response settings/schema))
  (POST "/_settings/upload" {body :body}
        (println "Updating Settings: " body)
        (settings/update-settings body)
        (response {:ok "updated"}))

  (route/not-found (response {:error {:code 404 :text "Not found"}})))


;; start-stop

(def phoenix-server (atom nil))

(defn start-phoenix
  []
  (let [server (http/start-server (-> app
                                   wrap-json-body
                                   wrap-json-response)
                     {:port @api-port})]
    (reset! phoenix-server server)))

(defn stop-phoenix
  []
  (when @phoenix-server
    (.close @phoenix-server)))


(defn authenticate
  []
  (-> (http/post (str @api-server "/api/v1/public/login")
                 {:pool @http-pool
                  :body (json/write-str
                          {:email @phoenix-email
                           :password @phoenix-password
                           :org "tenant"})
                  :content-type :json})
   deref
   :headers
   (get "jwt")))


(defn register-plugin
  []
  (if @api-server
    (try
      (let [resp (-> (http/post
                       (str @api-server "/api/v1/plugins/register")
                       {:pool @http-pool
                        :body (json/write-str @plugin-info)
                        :content-type :json
                        :headers {"JWT" (authenticate)}})
                     deref
                     :body
                     bs/to-string
                     json/read-str)]
        (settings/update-settings (get resp "settings")))
      (catch Exception e (println "Can't register plugin at phoenix" e)))
    (println "Phoenix address not set, can't register myself into phoenix :(")))

