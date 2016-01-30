(ns cljs-pixi-parallax.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljsjs.pixi]
            [goog.dom :as dom]
            [cljs.core.async :refer [<!] :as async]))

(enable-console-print!)

(def canvas-id "game-canvas")

(defprotocol Displayable
  (get-obj [this]
           "Applies all of the record's properties to the underlying Pixi.js object
           and returns it."))

(defrecord Container [pixi-obj children]
  Displayable
  (get-obj [_]
    (let [obj-num-children (aget pixi-obj "children" "length")]
      (if (not= obj-num-children (count children))
        (do
          (.removeChildren pixi-obj)
          (doseq [child children]
            (.addChild pixi-obj (get-obj child))))
        (doseq [i (range obj-num-children)]
          (.setChildIndex pixi-obj (get-obj (nth children i)) i)))
      pixi-obj)))

(defrecord ScrollerSprite [pixi-obj texture width height tile-pos-x pos-y]
  Displayable
  (get-obj [_]
    (aset pixi-obj "y" pos-y)
    (aset pixi-obj "tilePosition" "x" tile-pos-x)
    pixi-obj))

(defn mk-container [children]
  (Container. (js/PIXI.Container.) children))

(defn mk-scroller-sprite [texture width height tile-pos-x pos-y]
  (let [sprite (js/PIXI.extras.TilingSprite. texture width height)]
    (ScrollerSprite. sprite texture width height tile-pos-x pos-y)))

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {}))

(defn update-sprite-positions
  [state]
  (-> state (update-in [:scroller :children 0 :tile-pos-x] - 0.128)
            (update-in [:scroller :children 1 :tile-pos-x] - 0.64)))

(defn render!
  [{:keys [renderer scroller] :as state}]
  (.render renderer (get-obj scroller))
  state)

(defn new-renderer []
  (let [count (:__figwheel_counter @app-state)]
    (fn this []
      (when (= (:__figwheel_count @app-state) count)
        (js/requestAnimationFrame this)
        (swap! app-state
               #(-> %
                    update-sprite-positions
                    render!))))))

(defn load-resources!
  [c]
  (-> (js/PIXI.loaders.Loader.)
      (.add "bg-far" "img/bg-far.png")
      (.add "bg-mid" "img/bg-mid.png")
      (.load (fn [loader resources]
               (async/put! c resources)
               (async/close! c)))))

(defn init-scroller
  [state resources]
  (assoc state
    :scroller (mk-container
                [(mk-scroller-sprite
                   (aget resources "bg-far" "texture") 544 320 0 0)
                 (mk-scroller-sprite
                   (aget resources "bg-mid" "texture") 544 208 0 112)])))

(defn init-pixi
  [state]
  (let [canvas (dom/getElement canvas-id)
        width (.getAttribute canvas "width")
        height (.getAttribute canvas "height")]
    (assoc state
      :container (js/PIXI.Container.)
      :renderer (.autoDetectRenderer js/PIXI width height #js {:view canvas}))))

(defonce boot!
  (go (let [c (async/chan)]
        (load-resources! c)
        (let [resources (<! c)]
          (swap! app-state
                 #(-> %
                      init-pixi
                      (init-scroller resources))))
        ((new-renderer)))))

(defn on-js-reload []
  (swap! app-state update-in [:__figwheel_counter] inc)
  ((new-renderer)))
