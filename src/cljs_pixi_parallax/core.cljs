(ns cljs-pixi-parallax.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljsjs.pixi]
            [cljs.core.async :refer [<!] :as async]))

(enable-console-print!)

(def game-canvas (.getElementById js/document "game-canvas"))
(def canvas-width (.-width game-canvas))
(def canvas-height (.-height game-canvas))

(defonce container (js/PIXI.Container.))

(defonce renderer
         (.autoDetectRenderer js/PIXI
                              canvas-width
                              canvas-height
                              #js {:view game-canvas}))

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))

(defn new-renderer []
  (let [count (:__figwheel_counter @app-state)]
    (fn this []
      (let [{:keys [sprites __figwheel_counter]} @app-state
            far-x (aget (:bg-far sprites) "tilePosition" "x")
            mid-x (aget (:bg-mid sprites) "tilePosition" "x")]
        (when (= __figwheel_counter count)
          (js/requestAnimationFrame this)
          (aset (:bg-far sprites) "tilePosition" "x"
                (- far-x 0.128))
          (aset (:bg-mid sprites) "tilePosition" "x"
                (- mid-x 0.64))
          (.render renderer container))))))

(defn completed-loading-resources!
  [resources]
  (println "Resources loaded!")
  (let [bg-far (js/PIXI.extras.TilingSprite.
                 (aget resources "bg-far" "texture") 544 320)
        bg-mid (js/PIXI.extras.TilingSprite.
                 (aget resources "bg-mid" "texture") 544 208)]
    (.addChild container bg-far)
    (.addChild container bg-mid)
    (set! (.-y bg-mid) 112)
    (swap! app-state assoc-in [:sprites] {:bg-far bg-far
                                          :bg-mid bg-mid})))

(defn load-resources!
  [c]
  (-> (js/PIXI.loaders.Loader.)
      (.add "bg-far" "img/bg-far.png")
      (.add "bg-mid" "img/bg-mid.png")
      (.load (fn [loader resources]
               (async/put! c resources)
               (async/close! c)))))

(defonce boot!
  (go (let [c (async/chan)]
        (load-resources! c)
        (let [resources (<! c)]
          (completed-loading-resources! resources)
          ((new-renderer))))))

(defn on-js-reload []
  (swap! app-state update-in [:__figwheel_counter] inc)
  ((new-renderer)))
