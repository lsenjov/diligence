(ns diligence.core
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET POST]]
            ))

(enable-console-print!)

(println "This text is printed from src/diligence/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

;; People is a vector of people
(def app-state (atom {:people {1 {:first-name "Test" :last-name "Name"}
                               2 {:first-name "John" :last-name "Smith"}
                               }
                      :selected 1
                      :last-selection 2
                      }))


(defn get-data-on-person
  [id {:keys [first-name last-name website] :as request}]
  (println "Getting data on person. Id: " id " request: " (pr-str request))
  (GET "http://data.gov.au/api/action/datastore_search_sql"
       {:params {:sql (str "SELECT * from \"741da9e3-7e0c-458e-830c-c518698e1788\" WHERE 'BD_PER_NAME' LIKE " first-name " AND 'BD_PER_NAME' LIKE " last-name ";")}
        :handler #(println "Query return: " (pr-str %))
        :headers {
                  "Access-Control-Allow-Origin" "*"
                  "Access-Control-Allow-Headers" "Content-Type"
                  }
        }
       )
  )

(defn search-component
  []
  (let [query-atom (atom {})
        item-component (fn [kw s] [:div
                                   [:label {:class "control-label"} s]
                                   [:input {:class "form-control" :type "text" :on-change #(swap! query-atom assoc kw (-> % .-target .-value)) :label s}]
                                   ])
        ]
    (fn []
      [:div {:class "col-lg-4"}
       [:form {:class "form-horizontal"}
        (map item-component
             [:first-name :last-name :website]
             ["First name" "Last name" "Website"]
             )
        ]
       [:div {:class "btn btn-default"
              :onClick #(let [request @query-atom]
                          (reset! query-atom {})
                          (->
                            (swap! app-state
                                   (comp
                                     (fn [a] (assoc-in a [:people (:last-selection a)] request))
                                     (fn [a] (update-in a [:last-selection] inc))
                                     )
                                   )
                            :last-selection
                            (get-data-on-person request))
                          )
              }
        "Search"]
       (pr-str @query-atom)
       ]
      )))


(defn hello-world []
  [:div {:class "container"}
   [:h1 "Test"]
   ;; Person selector
   [:div {:class "btn-toolbar"}
    [:div {:class "btn-group"}
     (map (fn [[id {:keys [first-name last-name]}]]
            [:div {:class (if (= (:selected @app-state) id) "btn btn-primary" "btn btn-default")
                   :onClick #(swap! app-state assoc :selected id)
                   }
             (str last-name ", " first-name)])
          (:people @app-state))
     ]
    ]
   [search-component]
   (pr-str @app-state)
   ]
  )

(reagent/render-component [hello-world]
                          (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
