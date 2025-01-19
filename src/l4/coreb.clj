(ns l4.coreb
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [compojure.route :as route]
            [ring.middleware.cors :refer [wrap-cors]]
            [compojure.core :refer [defroutes POST GET]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer [response]]
            [next.jdbc :as jdbc]
            [clojure.java.jdbc :as jdb]
            [org.httpkit.server :as http]
            [reagent.core :as reagent]
            [clojure.data.json :as jso]
            [clojure.core.async :as async :refer [chan go put! take!]]
            [next.jdbc.sql :as sql]
            )
  (:import (java.util UUID)))

;; Настройки подключения к базе данных PostgreSQL
(def db-spec
  {:dbtype "postgresql"
   :host "localhost" ; Замените на адрес вашего сервера
   :port 5433        ; Порт по умолчанию для PostgreSQL
   :dbname "studs" ; Имя вашей базы данных
   :user "s291485"   ; Имя пользователя
   :password "qz6XMfGNKAgNOWkq"}) ; Пароль пользователя

;; Функция для выполнения запроса
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
                :id
                ["id = ? AND username = ?" id username]))
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
               (response {:users users})))
           (GET "/api/getchatsbyuser/:username" [username]
             (let [chats (fetch-chats username)]
               (response {:chats chats})))
           )
(def clients (atom {}))

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
                                            ))))))

(def ap
  (-> app-route
      wrap-json-body
      wrap-json-response
      (wrap-cors :access-control-allow-origin #".*"
                 :access-control-allow-methods [:get :post :options])))
(defn -main [& args]
  (http/run-server (fn [request]
               (if (= (:uri request) "/")
                 (websocket-handler request)
                 (ap request))) {:port 3002 }))
