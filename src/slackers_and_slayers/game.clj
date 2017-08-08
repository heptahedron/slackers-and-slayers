(ns slackers-and-slayers.game
  (:require [clojure.string :as string]
            [slackers-and-slayers.slack :as slack]))

(defn event-handler [event]
  (if (slack/is-command? event)
    [(slack/make-msg (:channel event) (slack/command-text event))
     (slack/make-msg (:channel event) "That's what you sound like.")]
    []))

(defn wrap-handler [f]
  (fn wrapped-handler [event]
    (if (slack/is-command? event)
      [(slack/make-msg (:channel event) (f {:user "human"
                                      :command (slack/command-text event)}))])))

(defn damage-text [{user :user command :command}]
    (if-let [int-str (re-matches #"\d+" command)]
        (str user " dealt " int-str " damage!")
        (str ":robot_face: `does not compute`")))

;; I cast magic missile on monster
;; I attack monster with my axe
;; I attack monster with magic missile
;; I heal player

(defn word [the-word]
  (fn word-matcher [some-text]
    (if (string/starts-with? some-text the-word)
      {:success the-word
       :rest (subs some-text (count the-word))}
      {:failure (str "Expected '" the-word "', but got something else.")})))

(defn in-order [parser1 parser2]
  (fn ordered-parsers [some-text]
    (let [result1 (parser1 some-text)]
      (if (contains? result1 :success)
        (let [result2 (parser2 (:rest result1))]
          (if (contains? result2 :success)
            {:success [(:success result1) (:success result2)]
             :rest (:rest result2)}
            result2))
        result1))))

(defn either [parser1 parser2]
  (fn choice-parser [some-text]
    (let [result1 (parser1 some-text)]
      (if (contains? result1 :success)
        result1
        (let [result2 (parser2 some-text)]
          (if (contains? result2 :success)
            result2
            {:failure "Couldn't parse either choice :((("}))))))
