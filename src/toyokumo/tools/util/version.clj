(ns toyokumo.tools.util.version
  (:require
   [rewrite-clj.zip :as z]
   [semver.core :as v])
  (:import
   (java.time
    Instant)))

(defn- move-zloc-to-root
  [zloc]
  (loop [zloc zloc]
    (if-let [zloc' (z/up zloc)]
      (recur zloc')
      zloc)))

(defn- update-value-string [zloc value-symbol f]
  (some-> zloc
          (z/find-value z/next value-symbol)
          (z/right)
          (z/edit f)))

(defn- assert-semver [v]
  (assert (v/valid? v) "Must be semantic version"))

(defn update-version-file! [file-path f]
  (let [zloc (z/of-file file-path)
        update-version-f (fn [v]
                           (assert-semver v)
                           (let [v' (f v)]
                             (assert-semver v')
                             v'))
        content (some-> zloc
                        (update-value-string 'version update-version-f)
                        (move-zloc-to-root)
                        (update-value-string 'generated-at (constantly (str (Instant/now))))
                        (z/root-string))]
    (when content
      (spit file-path content))))

(defn get-version [version-file]
  (let [version (some-> (z/of-file version-file)
                        (z/find-value z/next 'version)
                        (z/right)
                        (z/value))]
    (assert (v/valid? version) "Must be semantic version")
    version))
