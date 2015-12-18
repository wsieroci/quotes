(ns quotes.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [cljsjs.react :as react]
            [clojure.string :as string]
            [cljs.core.async :refer [put! chan <! timeout]])
  (:import goog.History))

(def sentances [{:text "You can do anything, but not everything." 
                :author "David Allen"} 
                {:text "Perfection is achieved, not when there is nothing more to add, but when there is nothing left to take away." 
                :author "Antoine de Saint-Exupéry"}
                {:text "A wise man gets more use from his enemies than a fool from his friends." 
                :author "Baltasar Gracian"}
                {:text "We are what we repeatedly do; excellence, then, is not an act but a habit." 
                :author "Aristotle"}
                {:text "What we think, or what we know, or what we believe is, in the end, of little consequence. The only consequence is what we do." 
                :author "John Ruskin"}])

;; -------------------------
(def state (atom {:sentance nil :isAuthorVisible false :time-left nil}))

(defn get-random-sentance [] (rand-nth sentances))

(defn next-sentance [] 
  (swap! state assoc :sentance (get-random-sentance))
  (swap! state assoc :isAuthorVisible false)
  (swap! state assoc :time-left 5000))

(defn decrease-time-left [time-left] 
    (swap! state assoc :time-left (- time-left 100)))

(defn show-author [] (swap! state assoc :isAuthorVisible true))

(defn simulate-tick []
  (go (while true
        (<! (timeout 100))
        (let [time-left (:time-left @state)]
          (if (> time-left 0)
            (decrease-time-left time-left)
            (next-sentance))))))

(defn sentance-component [sentance author-visible]
  (let [author (:author sentance)
        sentance-text (:text sentance)]
    [:div
      (if (string/blank? sentance-text)
            [:div.info "Please click next to show quote."]
            [:div.sentance sentance-text])

      (if author-visible
        [:div.author author])]))

(defn home-page []
  (let [sentance (:sentance @state)
        author-visible (:isAuthorVisible @state)
        time-left (:time-left @state)]
    [:div
      [:h2 "Meet famous quotes!"]

      [:div.toolbar
        [:button.button {:onClick next-sentance} "Next, please!"]
        [:button.button {:onClick show-author} "Show author"]
        (if (> time-left 0)
          [:span.time-left "Time left to auto-next: " time-left])]

      (sentance-component sentance author-visible)]))

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen EventType/NAVIGATE
                   (fn [event] (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-root []
  (simulate-tick)
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))