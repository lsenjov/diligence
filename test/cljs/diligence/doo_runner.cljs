(ns diligence.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [diligence.core-test]))

(doo-tests 'diligence.core-test)

