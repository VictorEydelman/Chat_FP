(ns l4.wsf
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [clojure.string :as str]))


(defonce app-state (r/atom {:messages []
                            :input-text ""
                            :socket nil}))


(defn send-message []
  (let [socket (:socket @app-state)
        text   (:input-text @app-state)]
    (when (and socket (not (str/blank? text)))
      (try
        (.send socket text)
        (swap! app-state assoc :input-text "")
        (catch js/Error e (js/console.log "Error sending message:" e))))))


(defn message-item [{:keys [text timestamp]}]
  [:p (str timestamp ":    " text)])


(defn chat-component []
  (let [messages (r/cursor app-state [:messages])
        input-text (r/cursor app-state [:input-text])]
    (fn []
      [:div
       "qqqqqqwwew"
       [:div#chat
        (for [msg @messages]
          ^{:key (random-uuid)}
          [message-item msg])
        ]
       [:input#message
        {:type        "text"
         :value       @input-text
         :placeholder "Enter message"
         :on-change   (fn [e] (swap! app-state assoc :input-text (.. e -target -value)))
         :on-key-up (fn [e]
                      (when (= 13 (.-keyCode e))
                        (send-message)))}]
       [:button
        {:name    "send-btn"
         :on-click #(send-message)}
        "Send"]
       (r/reaction
         (let [socket (.-socket @app-state)]
           (when (nil? socket)
             (let [new-socket (js/WebSocket. "ws://localhost:3002/ws?foo=clojure")]
               (set! (.-onopen new-socket)
                     (fn [_] (js/console.log "Connection established...")))
               (set! (.-onmessage new-socket)
                     (fn [event]
                       (try
                         (let [response (js/JSON.parse (.-data event))
                               timestamp (new js/Date)]
                           (when (and response (not (nil? (.-key response))) (= (.-key response) "chat"))
                             (swap! app-state update :messages conj {:text (.-data response)
                                                                     :timestamp (.toLocaleString timestamp)})))
                         (catch js/Error e (js/console.log "Error parse " e)))
                       ))
               (set! (.-onclose new-socket)
                     (fn [event]
                       (if (.-wasClean event)
                         (js/console.log "Connection closed. Clean exit.")
                         (js/console.log (str "Code: " (.-code event) ", Reason: " (.-reason event))))))

               (set! (.-onerror new-socket)
                     (fn [event]
                       (js/console.log (str "Error: " (.-message event)))
                       (.close new-socket)
                       )
                     )
               (swap! app-state assoc :socket new-socket)
               )
             )
           ))
       ])))