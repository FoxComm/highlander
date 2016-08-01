(ns utils.ring-json
  "Source from ring/ring-json. Main difference is another json library"
   (:require
     [pjson.core :as json]
     [ring.util.response :refer [content-type response]]))

(defn wrap-json-response
  "Set Content-Type for any responses to JSON"
  [handler]
  (fn [req]
    (if-let [resp (handler req)]
       (if (coll? (:body resp))
         (let [json-response (update-in resp [:body] json/write-str)]
           (content-type json-response "application/json"))
         resp))))

(defn- json-request? [request]
  (if-let [type (get-in request [:headers "content-type"])]
    (not (empty? (re-find #"^application/(.+\+)?json" type)))))

(def ^{:doc "The default response to return when a JSON request is malformed."}
  default-malformed-response
  {:status  400
   :headers {"Content-Type" "application/json"}
   :body    (json/write-str {:error {:text "Mailfored json request"}})})


(defn- read-json [request]
  (if (json-request? request)
    (if-let [body (:body request)]
      (let [body-string (slurp body)]
          (try
           [true (json/parse-string body-string)]
           (catch Exception ex
            [false nil]))))))


(defn wrap-json-body
  "Middleware that parses the body of JSON request maps, and replaces the :body
  key with the parsed data structure. Requests without a JSON content type are
  unaffected.
  Accepts the following options:
  :malformed-response - a response map to return when the JSON is malformed"
  {:arglists '([handler] [handler options])}
  [handler & [{:keys [malformed-response]
               :or {malformed-response default-malformed-response}}]]
  (fn [request]
    (if-let [[valid? json]
             (read-json request)]
      (if valid?
        (handler (assoc request :body json))
        malformed-response)
      (handler request))))
