(ns diligence.routes.home
  (:require [diligence.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [diligence.db.core :as db]
            ))

(defn home-page []
  (layout/render "home.html"))

(defn wrap-mysql-wildcards
  [s]
  (str \% s \%))

(defroutes home-routes
  (GET "/" []
       (home-page))
  (GET "/docs" []
       (-> (response/ok (-> "docs/docs.md" io/resource slurp))
           (response/header "Content-Type" "text/plain; charset=utf-8")))
  (GET "/api/edn/get-banned" {{:keys [first-name last-name] :as params} :params}
       (do (println "Params: " (pr-str params))
           (pr-str (-> params
                       (update-in [:first-name] wrap-mysql-wildcards)
                       (update-in [:last-name] wrap-mysql-wildcards)
                       db/select-persons))))
  )

