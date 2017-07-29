(ns diligence.routes.home
  (:require [diligence.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [diligence.db.core :as db]
            [clj-json.core :as json]
            [clojure.xml :as xml]
            [e85th.gaia.core :as gaia]
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
;; Not implemented, not enough time to get the SOAP working
(defn abn-call
  [business-name]
  (let [res (-> "http://abr.business.gov.au/abrxmlsearch/AbrXmlSearch.asmx?WSDL"
                ;slurp
                xml/parse
                )]
    res))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Finding nearest police station
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def station-list
  (let [fr (-> "QPS_STATIONS.shp"
                       gaia/shape-file->data-store
                       .getFeatureReader)]
    (take-while identity ;until we start getting nils
                (repeatedly
                  (fn [] (if (.hasNext fr)
                           (-> fr
                               .next
                               (.getAttributes)
                               ((fn [[p n]] {:x (.getX p) :y (.getY p) :name n}))
                               )))))))
(defn- get-current-location
  [s]
  (-> "http://maps.googleapis.com/maps/api/geocode/json?address="
      (str s)
      slurp
      json/parse-string
      (get-in ["results" 0 "geometry" "location"])
      ((fn [{lat "lat" lng "lng"}] {:x lng :y lat}))
      ))
(defn- get-distance
  [{cur-x :x cur-y :y} {tar-x :x tar-y :y tar-name :name}]
  (let [dist (Math/sqrt (+ (Math/pow (- (double cur-x) (double tar-x)) 2) (Math/pow (- (double cur-y) (double tar-y)) 2)))]
    ;(printf "Current x and y: %f %f Target x, y, and name: %f %f %s Distance: %f\n" cur-x cur-y tar-x tar-y tar-name dist)
    {:distance dist
     :name tar-name}))
(defn get-closest-station
  [s]
  (->> station-list
       (map (partial get-distance (get-current-location s)))
       (sort-by :distance)
       first
       :name
       ))
(comment
  (whois-call "yahoo.com")
  (abn-call "")
  (get-current-location "Ipswich+Queensland")
  (get-current-location "Brisbane+Queensland")
  (get-current-location "Brassall+Queensland")
  (get-closest-station "Brisbane+Queensland")
  (get-closest-station "Ipswich+Queensland")
  (get-closest-station "Brassall+Queensland")
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
  (GET "/api/edn/get-nearest-police" {{:keys [loc]} :params}
       (pr-str (get-closest-station loc)))
  )

