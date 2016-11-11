(ns messaging.main
 (:require
   [messaging.core :refer [start-react-app start-app stop-app]]
   [messaging.phoenix :refer [start-phoenix stop-phoenix register-plugin]]
   [clojure.core.async
    :as async
    :refer [<!! thread]])
 (:gen-class))

(defn -main
 [& args]
 (start-phoenix)
 (register-plugin)
 (let [react-app nil]
    ;; Clean up on a SIGTERM or Ctrl-C
   (println "react app started")

   (.addShutdownHook (Runtime/getRuntime)
                     (Thread. #(do
                                 (stop-phoenix)
                                 (stop-app))))
   (<!! (thread (start-app react-app)))))

