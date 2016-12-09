(ns messaging.phoenix
  (:require [aleph.http :as http]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [messaging.settings :as settings]
            [ring.util.response :refer [response]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [cheshire.core :as json]
            [byte-streams :as bs]
            [taoensso.timbre :as log]
            [environ.core :refer [env]]))

(def description "Provides messaging integration with Mailchimp/Mandrill")

(defn parse-int [s]
  (if (integer? s)
    s
    (Integer/parseInt s)))

(def api-port (delay (parse-int
                       (or
                         (:port env)
                         15054))))

(def api-host (delay (:api-host env)))

(def phoenix-url (delay (:phoenix-url env)))
(def phoenix-user (delay (:phoenix-user env)))
(def phoenix-password (delay (:phoenix-password env)))


(def http-pool (delay (http/connection-pool
                        {:connection-options {:insecure? true}})))


(defroutes app
  (GET "/_settings/schema" [] (response settings/schema))
  (GET "/_ping" [] (response {:ok "pong"}))
  (POST "/_set-log-level" {body :body}
    (let [level (some-> body
                  (get "level")
                  keyword)]
      (if (log/valid-level? level)
        (do
          (log/info "Set log level to" level)
          (log/set-level! level)
          (response {:result "ok"}))
        (do
          (log/error "Can't set log level to invalid level" level)
          {:status 400
           :headers {}
           :body {:result (str "invalid log level: " level)}}))))
  (POST "/_settings/upload" {body :body}
      (log/info "Updating Settings: " body)
      (settings/update-settings body)
      (response {:ok "updated"}))

  (route/not-found (response {:error {:code 404 :text "Not found"}})))


;; start-stop

(def phoenix-server (atom nil))

(defn start-phoenix
  []
  (log/info (str "Start HTTP API at :" @api-port))
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
  (-> (http/post (str @phoenix-url "/v1/public/login")
                 {:pool @http-pool
                  :body (json/generate-string
                          {:email @phoenix-user
                           :password @phoenix-password
                           :org "tenant"})
                  :content-type :json})
   deref
   :headers
   (get "jwt")))

(defn get-order-info
  [order-ref]
  (let [jwt (authenticate)
        request (http/get (str @phoenix-url "/v1/orders/" order-ref)
                  {:pool @http-pool
                   :headers {"JWT" jwt}
                   :content-type :json})
        resp (-> request
                 deref
                 :body
                 bs/to-string
                 json/parse-string)]
    resp))



(defn register-plugin
  [schema]
  (when (empty? @phoenix-url)
    (log/error "Phoenix address not set, can't register myself into phoenix :(")
    (throw (ex-info "$PHOENIX_URL is empty" {})))
  (try
    (log/info "Register plugin at phoenix" @phoenix-url)
    (let [plugin-info {:name "messaging"
                       :description description
                       :apiHost @api-host
                       :version "1.0"
                       :apiPort @api-port
                       :schemaSettings schema}
          resp (-> (http/post
                         (str @phoenix-url "/v1/plugins/register")
                         {:pool @http-pool
                          :body (json/generate-string plugin-info)
                          :content-type :json
                          :headers {"JWT" (authenticate)}})
                   deref
                       :body
                       bs/to-string
                       json/parse-string)]
      (log/info "Plugin registered at phoenix, resp" resp)
      (settings/update-settings (get resp "settings")))
    (catch Exception e
      (try
        (let [error-body (-> (ex-data e) :body bs/to-string)]
          (log/error "Can't register plugin at phoenix" error-body))
        (catch Exception einner
          (log/error "Can't register plugin at phoenix" e)))
      (throw e))))

