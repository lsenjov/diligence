(ns ^:figwheel-no-load diligence.app
  (:require [diligence.core :as core]
            [devtools.core :as devtools]))

(enable-console-print!)

(devtools/install!)

(core/init!)
