(ns messaging.shared
  (:require
   [environ.core :refer [env]]))

(def staging "staging")
(def environment (delay (:environment env)))
