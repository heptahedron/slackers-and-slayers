(ns slackers-and-slayers.slack
  (:require [aleph.http :as http]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [byte-streams :as bs]
            [clojure.data.json :as json]
            [clojure.string :as string]))

(def slack-rtm-connect-url "https://slack.com/api/rtm.connect")
(def ping-period-ms 10000)
(def next-msg-id (atom 0))

(def bot-id (atom nil))

(defn json-str->api-event [json-str]
  (json/read-str json-str :key-fn #(-> %
                                       (string/replace \_ \-)
                                       keyword)))

(defn api-event->json-str [api-event]
  (json/write-str api-event :key-fn #(-> %
                                         name
                                         (string/replace \- \_))))

(defn init-connect [bot-token]
  (d/chain (http/post slack-rtm-connect-url
                      {:form-params
                       {:token bot-token}})
           :body
           bs/to-string
           json-str->api-event))

(defn make-event [type body]
  (let [id (swap! next-msg-id inc)]
    (merge body {:id id :type type})))

(defn make-msg [channel text]
  (make-event "message" {:channel channel
                         :text text}))

(defn with-next-id [msg] (assoc msg :id (swap! next-msg-id inc)))

(defn ping []
  (make-event "ping" {}))

(defn wrap-duplex [going-in coming-out stream]
  (let [incoming (s/stream)]
    (s/connect (s/map going-in incoming) stream)
    (s/splice incoming (s/map coming-out stream))))

(defn annotate-trace [trace-msg]
  (fn trace-wrapper [x]
    (println trace-msg (pr-str x))
    x))

(defn start-client! [bot-token]
  (d/let-flow [{msg-server-url :url
                {bot-id- :id} :self} (init-connect bot-token)

               msg-stream (http/websocket-client msg-server-url)
               api-event-stream (wrap-duplex api-event->json-str
                                             json-str->api-event
                                             msg-stream)

               api-event-stream (wrap-duplex (annotate-trace "->")
                                             (annotate-trace "<-")
                                             api-event-stream)

               ping-stream (s/periodically ping-period-ms
                                           ping)]
    (reset! bot-id bot-id-)
    (println "Bot ID is" @bot-id)
    (s/connect ping-stream api-event-stream)
    api-event-stream))

(defn is-command? [{msg-type :type
                    msg-text :text}]
  (and (= msg-type "message")
       (string/starts-with? msg-text (str "<@" @bot-id ">"))))

(defn command-text [{msg-text :text}]
  (let [dropped (-> msg-text
                    (string/index-of \>)
                    (+ 2)
                    (min (count msg-text)))]
    (subs msg-text dropped)))

(defn command-stream [api-event-stream]
  (->> api-event-stream
       (s/filter is-command?)
       (s/map #(assoc % :command (command-text %)))))

(defn run-client! [bot-token event-handler]
  @(d/let-flow [api-event-stream (start-client! bot-token)
                responses (s/mapcat event-handler api-event-stream)]
     (s/consume #(s/put! api-event-stream %) responses)))
