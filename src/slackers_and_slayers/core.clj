(ns slackers-and-slayers.core
  (:require [slackers-and-slayers.game :as game]
            [slackers-and-slayers.slack :as slack])
  (:gen-class))

(defn -main
  ([]
   (println "Must provide bot API token as argument."))
  ([bot-token]
   (slack/run-client! bot-token game/event-handler)))
