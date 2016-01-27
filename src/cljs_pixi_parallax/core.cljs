(ns cljs-pixi-parallax.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljsjs.pixi]
            [cljs.core.async :refer [<! ] :as async]
            [goog.dom :as dom]))

(enable-console-print!)

(def canvas-id "game-canvas")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))

(defn update-sprite-positions
  [{:keys [scroller] :as state}]
  (let [{:keys [bg-far-tiling-x bg-mid-tiling-x]} scroller]
    (-> state
        ;; TODO: this is gross-looking figure something else out
        (assoc-in [:scroller :bg-far-tiling-x] (- bg-far-tiling-x 0.128))
        (assoc-in [:scroller :bg-mid-tiling-x] (- bg-mid-tiling-x 0.64)))))

(defn new-renderer []
  (let [count (:__figwheel_counter @app-state)]
    (fn this []
      (let [{:keys [sprites renderer container scroller __figwheel_counter]}
            (swap! app-state update-sprite-positions)]
        (when (= __figwheel_counter count)
          (js/requestAnimationFrame this)
          (aset (:bg-far sprites) "tilePosition" "x"
                (:bg-far-tiling-x scroller))
          (aset (:bg-mid sprites) "tilePosition" "x"
                (:bg-mid-tiling-x scroller))
          (aset (:bg-mid sprites) "y"
                (:bg-mid-y scroller))
          (.render renderer container))))))

(defn load-resources!
  [c]
  (-> (js/PIXI.loaders.Loader.)
      (.add "bg-far" "img/bg-far.png")
      (.add "bg-mid" "img/bg-mid.png")
      (.load (fn [loader resources]
               (async/put! c resources)
               (async/close! c)))))

(defn add-sprites-to-container! []
  (let [{:keys [container sprites]} @app-state]
    (-> container
        (.addChild (:bg-far sprites))
        (.addChild (:bg-mid sprites)))))

(defn init-scroller
  [state]
  (assoc state
    :scroller {:bg-far-y 0
               :bg-far-tiling-x 0
               :bg-mid-y 112
               :bg-mid-tiling-x 0}))

(defn init-sprites
  [state resources]
  (merge state
         {:sprites {:bg-far (js/PIXI.extras.TilingSprite.
                              (aget resources "bg-far" "texture") 544 320)
                    :bg-mid (js/PIXI.extras.TilingSprite.
                              (aget resources "bg-mid" "texture") 544 208)}}))

(defn init-pixi
  [state]
  (let [canvas (dom/getElement canvas-id)
        width (.getAttribute canvas "width")
        height (.getAttribute canvas "height")]
    (assoc state
      :container (js/PIXI.Container.)
      :renderer (.autoDetectRenderer js/PIXI width height #js {:view canvas}))))

(defonce boot!
  (do
    (let [c (async/chan)]
      (load-resources! c)
      (go (let [resources (<! c)]
            (swap! app-state
                   (fn [state]
                     (-> state
                         init-pixi
                         (init-sprites resources)
                         init-scroller))))
        (add-sprites-to-container!)
        ((new-renderer))))))

(defn on-js-reload []
  (swap! app-state update-in [:__figwheel_counter] inc)
  ((new-renderer)))
