(ns user
 (:require
    [messaging.core :refer [start-app stop-app]]
    [messaging.settings :as settings]
    [messaging.phoenix :refer [start-phoenix stop-phoenix register-plugin]]
    [taoensso.timbre :as log]
    [clojure.core.async
     :as async
     :refer [<!! thread]]))