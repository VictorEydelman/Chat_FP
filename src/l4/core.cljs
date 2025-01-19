(ns l4.core
  (:require [ajax.core :refer [POST GET]]
            [cljs.reader :as reader]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]

            [clojure.string :as str]))

(defonce app-state (reagent/atom true))
(defonce chats (reagent/atom []))
(defonce selectedchat (reagent/atom -1))
(defonce users (reagent/atom []))
(defonce username (reagent/atom ""))


(defn getchats []
  (println @username)
  (when (not=  @username "")
    (let [url "http://localhost:3002/api/getchatsbyuser/"] ;; Замените на нужный URL
      (-> (js/fetch (str url @username)
                    (clj->js {:method "GET"
                              :headers {"Content-Type" "application/json"}}))
          (.then (fn [response]
                   (.json response)))
          (.then (fn [data]
                   (js/console.log "Полученные данные:" data)
                   (js/console.log "Полученные данные:" )
                   (reset! chats (.-chats data))
                   ))
          (.catch (fn [error]
                    (js/console.error "Ошибка при получении данных:" error)))))))
(defn getusers []
  (let [url "http://localhost:3002/api/users"] ;; Замените на нужный URL
    (-> (js/fetch url
                  (clj->js {:method "GET"
                            :headers {"Content-Type" "application/json"}}))
        (.then (fn [response]
                 (.json response)))
        (.then (fn [data]
                 (reset! users  (.-users data))
                 (js/console.log @users)
                 ))
        (.catch (fn [error]
                  (js/console.error "Ошибка при получении данных:" error)))))
  )
(defn avt [username password]
  (-> (js/fetch "http://localhost:3002/api/reg"
                   (clj->js {:method "POST"
                             :headers {"Content-Type" "application/json"}
                             :body (js/JSON.stringify (clj->js {:username username :password password}))
                             }))
      (.then (fn [response]
               (js/alert response)
               (if response
                 (.json response)
                 (js/Promise.reject "Ошибка сети"))))
      (.then (fn [data]
               (if (= (.-status data) "success")
                 (reset! app-state false) (js/alert (.-message data)) )

              (js/console.log "Полученные данные:" data)))
      (.catch (fn [error]
                (js/console.error "Ошибка при получении данных:" error))))
  )
(defn log [username password]
  (-> (js/fetch "http://localhost:3002/api/log"
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/json"}
                          :body (js/JSON.stringify (clj->js {:username username :password password}))
                          }))
      (.then (fn [response]
               (js/alert response)
               (if response
                 (.json response)
                 (js/Promise.reject "Ошибка сети"))))
      (.then (fn [data]
               (if (= (.-status data) "success")
                 (reset! app-state false) (js/alert (.-message data)) )
               (js/console.log "Полученные данные:" data)))
      (.catch (fn [error]
                (js/console.error "Ошибка при получении данных:" error))))
  )
(defn create [name username]
  (println name username)
  (-> (js/fetch "http://localhost:3002/api/create"
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/json"}
                          :body (js/JSON.stringify (clj->js {:name name :username username}))
                          }))
      (.then (fn [response]
               (if response
                 (.json response)
                 (js/Promise.reject "Ошибка сети"))))
      (.then (fn [data]
               (reset! app-state false)
               (js/console.log "Полученные данные:" data)))
      (.catch (fn [error]
                (js/console.error "Ошибка при получении данных:" error))))
  )
(defn addUserChat [id username]
  (-> (js/fetch "http://localhost:3002/api/adduserchat"
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/json"}
                          :body (js/JSON.stringify (clj->js {:id id :username username}))
                          }))
      (.then (fn [response]
               (js/alert response)
               (if response
                 (.json response)
                 (js/Promise.reject "Ошибка сети"))))
      (.then (fn [data]
               (reset! app-state false)
               (js/console.log "Полученные данные:" data)))
      (.catch (fn [error]
                (js/console.error "Ошибка при получении данных:" error))))
  )
(defn deleteUserChat [name username]
  (-> (js/fetch "http://localhost:3002/api/deleteuserchat"
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/json"}
                          :body (js/JSON.stringify (clj->js {:id name :username username}))
                          }))
      (.then (fn [response]
               (js/alert response)
               (if response
                 (.json response)
                 (js/Promise.reject "Ошибка сети"))))
      (.then (fn [data]
               (reset! app-state false)
               (js/console.log "Полученные данные:" data)))
      (.catch (fn [error]
                (js/console.error "Ошибка при получении данных:" error))))
  )
(defonce filtered-items (reagent/atom @users))
(defonce search-text (reagent/atom ""))
(defonce search-chat (reagent/atom ""))
(defonce message-input (reagent/atom ""))
(defonce is-open (reagent/atom false))
(def ws-url "ws://localhost:3002") ; Замените URL на адрес вашего сервера

(defonce ws (atom nil))
(defonce messages (reagent/atom []))
(defonce message (reagent/atom ""))
(defonce messageopen (reagent/atom true))



(defn on-message [msg]
  (let [data (js/JSON.parse msg)]
    (println "Сообщение от сервера:" data @selectedchat)
    (swap! messages conj data)
    ))

(defn start-websocket []
  (reset! ws (js/WebSocket. ws-url))
  (set! (.-onopen @ws)
        (fn [] (println "Соединение установлено Ok!")))
  (set! (.-onmessage @ws)
        (fn [event] (on-message (.-data event))))
  (set! (.-onclose @ws)
        (fn [] (println "Соединение закрыто close!")))
  (set! (.-onerror @ws)
        (fn [error] (println "Ошибка:" error))))
(defn send-message [message]
  (println message @ws)
  (when @ws
    (.send @ws (js/JSON.stringify (str message "," @username "," @selectedchat)))))

(defn send-button-click []
  (println @message)
  (when (not (empty? @message))
    (send-message @message)
    (reset! message "")))
(defn chat-ui []
  [:div
   [:h1 "WebSocket Chat"]
   [:input {:type "text" :value @message
            :on-change #(reset! message (-> % .-target .-value))}]
   [:button {:on-click send-button-click} "Send"]
   [:ul
    (for [m @messages]
      ^{:key m}
      (when (= (.-chat m) (str @selectedchat)) [:div (.-data m) (.-username m)]))]])
(defn filter-users-by-prefix [users prefix]
  (filter #(clojure.string/starts-with? (.-username %) prefix) users))
(defn filter-chats-by-prefix [chats prefix]
  (filter #(clojure.string/starts-with? (.-name %) prefix) chats))

(defn hello-world []
      (let [ password (reagent/atom "") chatname (reagent/atom "") message (reagent/atom "")]
           (fn []
             (reagent/create-class
               {:component-did-mount (fn [] (do (js/setInterval getchats 4000) (js/setInterval getusers 4000)))
                :reagent-render (fn []
                                  (if @app-state
                                    [:div {:style {:display "flex" :flex-direction "column" :align-items "center" :justify-content "center" :min-height "100vh" :background-color "#f9f9f9"}}
                                     [:form {:style {:display "flex" :flex-direction "column" :width "300px"}}
                                      [:div {:style {:margin-bottom "15px"}}
                                       [:label "Username: "]
                                       [:input {:type "text" :value @username
                                                :on-change #(reset! username (-> % .-target .-value))
                                                :style {:padding "10px" :border "1px solid #ccc" :border-radius "5px"}}]]
                                      [:div {:style {:margin-bottom "15px"}}
                                       [:label "Password: "]
                                       [:input {:type "password" :value @password
                                                :on-change #(reset! password (-> % .-target .-value))
                                                :style {:padding "10px"
                                                        :border "1px solid #ccc"
                                                        :border-radius "5px"}}]]
                                      [:button {:on-click (fn [e] (.preventDefault e) (avt @username @password))
                                                :style {:padding "10px" :background-color "#61dafb" :color "#fff" :border "none" :border-radius "5px" :cursor "pointer" :margin-bottom "10px"}} "Зарегистрироваться"]
                                      [:button {:on-click (fn [e] (.preventDefault e) (log @username @password))
                                                :style {:padding "10px" :background-color "#007bff" :color "#fff" :border "none" :border-radius "5px" :cursor "pointer"}} "Войти"]]]

                                    [:div
                                     [:div {:style {:width "20%" :float "left" :overflow "hidden"}}
                                      [:div {:style {:font-size "20px" :font-weight "bold" :margin-bottom "10px"}} @username]
                                     [:button {:on-click (fn [] (do (reset! app-state true) (reset! selectedchat -1) (reset! is-open false) (reset! search-text "")))
                                               :style {:padding "10px 15px" :background-color "#ff4d4d" :color "#fff" :border "none" :border-radius "5px" :cursor "pointer"}} "Выйти"]
                                     [:form {:on-submit (fn [e] (.preventDefault e) (create @chatname @username))
                                             :style {:margin-top "10px"}}
                                      [:div {:style {:margin-bottom "10px"}}
                                       [:label "Название: "]
                                       [:input {:type "text" :value @chatname
                                                :on-change #(reset! chatname (-> % .-target .-value))
                                                :style {:padding "10px" :border "1px solid #ccc" :border-radius "5px" :width "100%"}}]]
                                      [:button {:style {:padding "10px 15px" :background-color "#28a745" :color "#fff" :border "none" :border-radius "5px" :cursor "pointer"}} "Создать"]]
                                     [:ul {:style {:list-style-type "none"
                                                   :padding "0"}}
                                      [:input {:type "text"
                                               :value @search-chat
                                               :on-change #(reset! search-chat (-> % .-target .-value))
                                               :placeholder "Поиск..."
                                               :style {:padding "10px" :border "1px solid #ccc" :border-radius "5px" :width "100%"}}]
                                      (let [filter (filter-chats-by-prefix @chats @search-chat)]
                                        (for [chat filter]
                                        ^{:key chat}
                                        [:li {:on-click (fn [] (reset! selectedchat  (.-id chat)))
                                              :style {:padding "10px" :cursor "pointer" :border-bottom "1px solid #ddd"}} (.-name chat)]))]]

                                     [:div {:style {:width "70%" :float "right" :overflow "hidden"}} (if (not (== @selectedchat -1))
                                       [:div  (js/console.log @messageopen) [:div (when @messageopen (do (send-message "Database Data") (reset! messageopen false))) (chat-ui)]
                                        [:div {:style {:position "relative"}}
                                              [:button {:on-click #(swap! is-open not)
                                                        :style {:padding "10px 15px" :background-color "#007bff" :color "#fff" :border "none" :border-radius "5px" :cursor "pointer"}} "Добавить участника"]
                                         [:button {:on-click #(do (deleteUserChat @selectedchat @username) (reset! selectedchat -1))
                                                   :style {:padding "10px 15px" :background-color "#007bff" :color "#fff" :border "none" :border-radius "5px" :cursor "pointer"}} "Выйти из чата"]
                                              (when @is-open
                                                [:div
                                                 [:input {:type "text"
                                                          :value @search-text
                                                          :on-change #(reset! search-text (-> % .-target .-value))
                                                          :placeholder "Поиск..."
                                                          :style {:padding "10px" :border "1px solid #ccc" :border-radius "5px" :width "100%"}}]
                                                 [:div  {:style {:border "1px solid #ccc" :max-height "150px" :overflow-y "auto" :position "absolute" :background-color "#fff" :width "100%" :z-index 100}}
                                                  (let [filter-users (filter-users-by-prefix @users @search-text)]
                                                    (for [item filter-users]
                                                      ^{:key item}
                                                      [:div  {:on-click #(addUserChat @selectedchat  (.-username item))
                                                              :style {:padding "8px" :cursor "pointer"}}
                                                       (.-username item)]))]])]]
                                       [:div  {:style {:margin-top "10px" :color "#888"}}  "Выберете чат"])]
                                     ]))}))))

(defn init []
  (start-websocket)
  (rdom/render [hello-world] (.getElementById js/document "app")))

(init)