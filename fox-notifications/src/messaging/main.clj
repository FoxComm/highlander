(ns messaging.main
 (:require
   [messaging.core :refer [start-react-app start-app stop-app]]
   [messaging.settings :as settings]
   [messaging.phoenix :refer [start-phoenix stop-phoenix register-plugin]]
   [clojure.core.async
    :as async
    :refer [<!! thread]])
 (:gen-class))

(defn -main
 [& args]
 (start-phoenix)
 (register-plugin settings/schema)
 (let [react-app nil]
    ;; Clean up on a SIGTERM or Ctrl-C
   (println "react app started")

   (.addShutdownHook (Runtime/getRuntime)
                     (Thread. #(do
                                 (stop-phoenix)
                                 (stop-app))))
   (<!! (thread (start-app react-app)))))

