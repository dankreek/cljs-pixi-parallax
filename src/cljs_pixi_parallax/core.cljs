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


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
