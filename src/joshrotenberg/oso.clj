(ns joshrotenberg.oso
  (:require [clojure.java.io :as io])
  (:import [com.osohq.oso Oso]
           [java.util Map]))

(defn new-oso
  "Create a new Oso instance"
  []
  (Oso.))

(defn register-class
  "Register a class"
  [oso c name]
  (doto oso
    (.registerClass c name)))

(defn load-files
  "Load polar files"
  [oso paths]
  (doto oso
    (.loadFiles (into-array String paths))))

(comment
  ;; this is the bare minimum. it just happens to work using `Map` as the class type
  ;; because oso doesn't have to call any methods for this access path.
  (let [oso (new-oso)
        path (-> "test_oso.polar"
                 io/resource
                 io/file
                 .getAbsolutePath)]
    (doto oso
      (register-class Map "User")
      (register-class Map "Widget")
      (register-class Map "Company")
      (load-files [path]))
    (let [user {"name" "guest"}
          widget {"id" 1}]
      (assert (.isAllowed oso user "get" widget))))
  ;;
  )
