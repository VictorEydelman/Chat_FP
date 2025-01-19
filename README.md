# Лабораторная работа №4 по Функциональному программированию

Тема: Peer2Peer сервис чатов

Выполнил: Эйдельман Виктор Аркадьевич

## Функциональность:

1) Регистрация:
Можно загестрироваться или войти в аккаунт
![image](https://github.com/user-attachments/assets/7aa2339e-02ef-4aa3-aef9-6dfb3f1eae40)

2) Основная страница:
![image](https://github.com/user-attachments/assets/342d8e5b-9a19-403c-8f69-3f86dae4260b)

2.1) Можно выйти из аккаунта <br>
2.2) Можно создать чат<br>
2.3) Можно найти нужный чат по названию<br>
2.4) Можно выбрать чат:<br>
![image](https://github.com/user-attachments/assets/60f6d565-a89b-4948-8af3-3d07305d4bec)
2.4.1) Можно прочитать чат<br>
2.4.2) Отправить сообщение<br>
2.4.3) Добавить участника<br>
2.4.3.1) Поиск по пользователям<br>
2.4.4) Выйти из чата<br>

## Реализация:

1) Для реализации используюется бд для хранения всех пользователь, чатов, их истории и участников.
   
```clojure
(defn fetch-users []
  (jdb/query db-spec ["SELECT username FROM users_fp"]))
(defn fetch-messages []
  (jdb/query db-spec ["SELECT * FROM messages"]))
(defn fetch-chats [username]
  (jdb/query db-spec ["select c.* from chats c join chat_user cu on c.id=cu.id where cu.username=?" username]))
(defn insert-users [username password]
  (jdbc/with-transaction [tx db-spec]
                         (jdbc/execute! tx ["INSERT INTO users_FP (username, password) VALUES (?, ?)" username password])))
(defn add-chat [chat-name]
  (let [result (jdb/insert! db-spec
                             :Chats
                             {:name chat-name})]
    (-> result first :id)))
(defn insert-messages [id username message]
  (jdbc/with-transaction [tx db-spec]
                         (jdbc/execute! tx ["INSERT INTO messages (id,username,message) VALUES (?,?,?)" id username message])))
(defn insert-chat_user [id username]
  (jdbc/with-transaction [tx db-spec]
                         (jdbc/execute! tx ["INSERT INTO chat_user (id,username) VALUES (?, ?)" id username])))
(defn user-exists? [username]
  (let [query "SELECT COUNT(*) FROM users_FP WHERE username = ?"
        result (jdb/query db-spec [query username])]
    (-> result first :count pos?)))
(defn user-login? [username password]
  (let [query "SELECT COUNT(*) FROM users_FP WHERE username = ? and password = ?"
        result (jdb/query db-spec [query username password])]
    (-> result first :count pos?)))
(defn user-chat? [id username]
  (let [query "SELECT COUNT(*) FROM chat_user WHERE id = ? and username = ?"
        result (jdb/query db-spec [query id username])]
    (-> result first :count pos?)))
(defn delete-chat-user [id username]
  (jdb/delete! db-spec
               :chat_user
                ["id = ? AND username = ?" id username]))
```


2) Для передачи данных используется fetch api.

Бэк:
```clojure
(defroutes app-route
           (POST "/api/reg" request
             (let [data (:body request)]
               (if (not (user-exists? (get data "username")))
                 (do (insert-users (get data "username") (get data "password"))
                     (response {:status "success" :received data}))
                 (response {:status "Username exist" :received data}))
               ))
           (POST "/api/log" request
             (let [data (:body request)]
               (if (user-login? (get data "username")(get data "password"))
                 (response {:status "success" :received data})
                 (response {:status "User not exist" :received data}))
               ))
           (POST "/api/create" request
             (let [data (:body request) id (add-chat (get data "name"))]
               (println data)
               (insert-chat_user id (get data "username"))
               (response {:status "success" :received id})))
           (POST "/api/adduserchat" request
             (let [data (:body request)]
               (when (not (user-chat? (get data "id") (get data "username"))) (do (insert-chat_user (get data "id") (get data "username"))
               (response {:status "success" :received data})))))
           (POST "/api/deleteuserchat" request
             (let [data (:body request)]
               (println data)
               (delete-chat-user (get data "id") (get data "username"))
               (response {:status "success" :received data})))
           (GET "/api/users" []
             (let [users (fetch-users)]
               (println users)
               (response {:users users})))
           (GET "/api/getchatsbyuser/:username" [username]
             (let [chats (fetch-chats username)]
               (response {:chats chats})))
           )
```

Фронт:
```clojure
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
  (println 112)
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
```


3) Для передачи сообщений в чатах используются WebSocket.

Бэк:
```clojure
(defn send-message [msg]
  (println msg)
  (doseq [channel (vals @clients)]
    (http/send! channel msg)))

(defn websocket-handler [req]
  (println req)
  (http/with-channel req channel
                     (let [client-id (str (UUID/randomUUID))]
                       (println channel client-id)
                       (swap! clients assoc client-id channel)
                       (let [datas (fetch-messages)]
                         (for [i (range (count datas))]
                           (send-message (json/generate-string {:message "Database Data" :data (nth datas i)})))
                         )
                       (http/on-close channel (fn [] (swap! clients dissoc client-id) ()))
                       (http/on-receive channel
                                        (fn [text]
                                          (println text)
                                          (let [data (json/parse-string text true) d (str/split data #",") msg (nth d 0) user (nth d 1) chat (nth d 2)]
                                            (println msg 1 user 2 chat)
                                            (if (= msg "Database Data") (let [datas (fetch-messages)]
                                                                          (println datas)
                                                             (doseq [i (range (count datas))]
                                                               (send-message (json/generate-string {:message "Database Data" :data (:message (nth datas i)) :chat (str(:id (nth datas i))) :username (:username (nth datas i))}))))
                                                           (do (insert-messages (Integer/parseInt chat) user msg) (send-message (json/generate-string {:message "Received: " :data msg :chat chat :username user}))))
```

Фронт:
```clojure
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
    (reset! message "")))                      ))))))

```
