(ns diligence.routes.home
  (:require [diligence.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [diligence.db.core :as db]
            [clj-json.core :as json]
            ))

(defn home-page []
  (layout/render "home.html"))

(defn wrap-mysql-wildcards
  [s]
  (str \% s \%))

(defn whois-call
  [url]
  (let [res (-> "http://whoiz.herokuapp.com/lookup.json"
                (str "?url=" url)
                slurp
                json/parse-string
                (dissoc "disclaimer")
                )]
    (println "Whois call response: " (pr-str res))
    res)
  )
(comment
  (whois-call "yahoo.com")
  )

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
  (GET "/api/edn/get-whois" {{:keys [url]} :params}
       (pr-str (whois-call url)))
  )

