(ns joshrotenberg.oso
  "This is a sort of stream of consciousness Clojure file to see
  what a wrapper of the Oso Java library might look, starting out with
  just basic interop and attempting to come up with something a bit more
  idiomatic if possible."
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
                 .getPath)]
    (doto oso
      (register-class Map "User")
      (register-class Map "Widget")
      (register-class Map "Company")
      (load-files [path]))
    (let [user {"name" "guest"}
          widget {"id" 1}]
      (assert (.isAllowed oso user "get" widget))))

  ;; this block crashes with several exceptions:
  ;; resource.role(actor) = "admin" fails (among others)
  (let [oso (new-oso)
        path (-> "test_oso.polar"
                 io/resource
                 io/file
                 .getPath)]
    (doto oso
      (register-class Map "User")
      (register-class Map "Company")
      (load-files [path]))
    (let [user {"name" "president"}
          company {"id" 1}]
      (.isAllowed oso user "create" company))))

;; so obviously we need to register classes that can fulfill the contract 
;; we've told oso about. we can get close in clojure with defprotocol/defrecord

(declare joshrotenberg.oso.User)
(declare joshrotenberg.oso.Widget)
(declare joshrotenberg.oso.Company)

(defprotocol CompaniesProtocol (companies [_]))
(defprotocol RoleProtocol (role [this user]))

(defrecord User [name] CompaniesProtocol (companies [this] (Company. 1)))
(defrecord Widget [id])
(defrecord Company [id]
  RoleProtocol (role [this user] (if (= (:name user) "president")
                                   "admin"
                                   "guest")))
(comment
  ;; unfortunately this doesn't work. the record has the methods but its also a map
  ;; and we get this error because the map keys are keywords and not strings:
  ;; Execution error (Exceptions$UnexpectedPolarTypeError) at com.osohq.oso.Host/toPolarTerm (Host.java:234).
  ;; Cannot convert map with non-string keys to Polar
  (let [oso (new-oso)
        path (-> "test_oso.polar"
                 io/resource
                 io/file
                 .getPath)
        user (User. "president")
        company (Company. 1)]
    (doto oso
      (register-class joshrotenberg.oso.User "User")
      (register-class joshrotenberg.oso.Company "Company")
      (register-class joshrotenberg.oso.Widget "Widget"))
    (.isAllowed oso user "create" company)))
