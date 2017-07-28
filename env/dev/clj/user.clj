(ns user
  (:require [mount.core :as mount]
            [diligence.figwheel :refer [start-fw stop-fw cljs]]
            diligence.core))

(defn start []
  (mount/start-without #'diligence.core/repl-server))

(defn stop []
  (mount/stop-except #'diligence.core/repl-server))

(defn restart []
  (stop)
  (start))


