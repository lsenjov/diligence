(ns diligence.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[diligence started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[diligence has shut down successfully]=-"))
   :middleware identity})
