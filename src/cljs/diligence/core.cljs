(ns diligence.core
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [diligence.ajax :refer [load-interceptors!]]
            [ajax.core :refer [GET POST] :as ajax])
  (:import goog.History))

;; People is a vector of people
(defonce app-state (r/atom {:people {}
                            :selected 1
                            :last-selection 0
                            }))

(defn get-data-on-person
  [id {:keys [first-name last-name website] :as request}]
  (println "Getting data on person. Id: " id " request: " (pr-str request))
  (GET "/api/edn/get-banned"
       {:params request
        ;:response-format (ajax/transit-response-format)
        :handler #(let [ret (cljs.reader/read-string %)]
                    (println "Query return: " (pr-str ))
                    (swap! app-state assoc-in [:people id :banned-person] ret))
        }))

(defn search-component
  []
  (let [query-atom (r/atom {})
        item-component (fn [kw s]
                         ^{:key kw}
                         [:div
                          [:label {:class "control-label"} s]
                          [:input {:class "form-control"
                                   :type "text"
                                   :on-change #(swap! query-atom assoc kw (-> % .-target .-value))
                                   :value (get @query-atom kw)
                                   :label s}]])]
    (fn []
      [:div.col-lg-4
       [:form.form-horizontal
        (doall (map item-component
                    [:first-name :last-name :website]
                    ["First name" "Last name" "Website"]))]
       [:div.btn.btn-default
        {:onClick #(let [request @query-atom]
                     (reset! query-atom {})
                     (->
                       (swap! app-state
                              (comp
                                (fn [a] (assoc-in a [:people (:last-selection a)] request))
                                (fn [a] (update-in a [:last-selection] inc))))
                       :last-selection
                       (get-data-on-person request)))}
        "Search"]
       ])))

(defn banned-person-component
  []
  (if-let [res (get-in @app-state [:people (:selected @app-state) :banned-person])]
    ;; A record was returned
    [:div.col-lg-4
     [:div.panel.panel-default
      [:div.panel-heading "ASIC Banned List:"]
      [:div.panel-body
       (if (pos? (count res))
         [:table.table.table-striped.table-hover
          [:thead>tr
           [:th "Record Name"]
           [:th "Start Date"]
           [:th "End Date"]
           [:th "Ban Type"]
           [:th "Area"]
           ]
          [:tbody
          (doall (map (fn [id {:keys [bd_per_end_dt, bd_per_start_dt, bd_per_name, bd_per_add_country, bd_per_add_state, bd_per_add_local, bd_per_type]}]
                        ^{:key id}
                        [:tr
                         [:td bd_per_name]
                         [:td bd_per_start_dt]
                         [:td bd_per_end_dt]
                         [:td bd_per_type]
                         [:td (str bd_per_add_local ", " bd_per_add_state ", " bd_per_add_country)]
                         ])
                      (range)
                      (sort-by :bd_per_name res)))]])
       ]
      ]
     ]
    ))

(defn nav-link [uri title page collapsed?]
  [:li.nav-item
   {:class (when (= page (session/get :page)) "active")}
   [:a.nav-link
    {:href uri
     :on-click #(reset! collapsed? true)} title]])

(defn navbar []
  (let [collapsed? (r/atom true)]
    (fn []
      [:nav.navbar.navbar-dark.bg-primary
       [:button.navbar-toggler.hidden-sm-up
        {:on-click #(swap! collapsed? not)} "☰"]
       [:div.collapse.navbar-toggleable-xs
        (when-not @collapsed? {:class "in"})
        [:a.navbar-brand {:href "#/"} "diligence"]
        [:ul.nav.navbar-nav
         [nav-link "#/" "Home" :home collapsed?]
         [nav-link "#/about" "About" :about collapsed?]]]])))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src (str js/context "/img/warning_clojure.png")}]]]])

(defn home-page []
  [:div.container
   (when-let [docs (session/get :docs)]
     [:div.row>div.col-lg-12
   [:div.btn-toolbar
    [:div.btn-group
     (doall
       (map (fn [[id {:keys [first-name last-name]}]]
              ^{:key id}
              [:div {:class (if (= (:selected @app-state) id) "btn btn-primary" "btn btn-default")
                     :onClick #(swap! app-state assoc :selected id)
                     }
               (str last-name ", " first-name)])
            (:people @app-state)))
     ]
    ]
   [search-component]
   [banned-person-component]
   (pr-str @app-state)])])

(def pages
  {:home #'home-page
   :about #'about-page})

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/about" []
  (session/put! :page :about))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
        (events/listen
          HistoryEventType/NAVIGATE
          (fn [event]
              (secretary/dispatch! (.-token event))))
        (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET "/docs" {:handler #(session/put! :docs %)}))

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (load-interceptors!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
