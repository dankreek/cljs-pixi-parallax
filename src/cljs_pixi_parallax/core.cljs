(ns cljs-pixi-parallax.core
  (:require [cljsjs.pixi]))

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

(defn completed-loading-resources!
  [loader resources]
  (println "Resources loaded!")
  (let [bg-far (js/PIXI.Sprite. (aget resources "bg-far" "texture"))
        bg-mid (js/PIXI.Sprite. (aget resources "bg-mid" "texture"))]
    (.addChild container bg-far)
    (.addChild container bg-mid)
    (set! (.-y bg-mid) 112)
    (.render renderer container)))


(defn load-resources! []
  (-> (js/PIXI.loaders.Loader.)
      (.add "bg-far" "img/bg-far.png")
      (.add "bg-mid" "img/bg-mid.png")
      (.load completed-loading-resources!)))

(defonce boot!
  (do (load-resources!)))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)



