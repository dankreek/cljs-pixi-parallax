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

(defn render! []
  (js/requestAnimationFrame render!)
  (let [{:keys [sprites]} @app-state
        far-x (aget (:bg-far sprites) "tilePosition" "x")
        mid-x (aget (:bg-mid sprites) "tilePosition" "x")]
    (aset (:bg-far sprites) "tilePosition" "x"
          (- far-x 0.128))
    (aset (:bg-mid sprites) "tilePosition" "x"
          (- mid-x 0.64))
    (.render renderer container)))

(defn completed-loading-resources!
  [loader resources]
  (println "Resources loaded!")
  (let [bg-far (js/PIXI.extras.TilingSprite.
                 (aget resources "bg-far" "texture") 544 320)
        bg-mid (js/PIXI.extras.TilingSprite.
                 (aget resources "bg-mid" "texture") 544 208)]
    (.addChild container bg-far)
    (.addChild container bg-mid)
    (set! (.-y bg-mid) 112)
    (swap! app-state assoc-in [:sprites] {:bg-far bg-far
                                          :bg-mid bg-mid})
    (render!)))

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
