(ns slackers-and-slayers.core
  (:require [aleph.http :as http]
            [manifold.deferred :as d]
            [manifold.stream :as stream]
            [byte-streams :as bs]
            [clojure.data.json :as json])
  (:gen-class))

(def slack-rtm-connect-url "https://slack.com/api/rtm.connect")

(defn get-message-server-url [bot-token]
  (d/chain (http/post slack-rtm-connect-url
                      {:form-params {:token bot-token}})
           :body
           bs/to-reader
           json/read
           #(get % "url")))

(defn msg-handler [msg]
  (println msg))

(defn listen-msgs [bot-token]
  (d/let-flow [msg-server-url (get-message-server-url bot-token)]
    (println "Successfully obtained message server URL:" msg-server-url)
    (d/let-flow [conn (http/websocket-client msg-server-url)]
      (stream/consume msg-handler conn))))

(defn -main
  "I don't do a whole lot ... yet."
  ([one-arg]
   (println "Hello, World!"))
  ([one two]
   (println "two!")))
