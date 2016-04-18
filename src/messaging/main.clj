(ns messaging.main
 (:require
   [messaging.core :refer [start-react-app start-app stop-app]]
   [clojure.core.async
    :as async
    :refer [<!! thread]])
 (:gen-class))

(defn -main
 [& args]
 (let [react-app (start-react-app)]
    ;; Clean up on a SIGTERM or Ctrl-C
   (println "react app started")
   (.addShutdownHook (Runtime/getRuntime)
                     (Thread. #(do (stop-app))))
   (<!! (thread (start-app react-app)))))
