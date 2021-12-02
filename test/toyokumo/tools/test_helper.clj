(ns toyokumo.tools.test-helper
  (:require
   [clojure.java.io :as io])
  (:import
   (java.io
    File)))

(def test-version-file
  (.getAbsolutePath (File/createTempFile "tools.release" "version.clj")))

(defn test-version-file-fixture [f]
  (let [file (io/file test-version-file)]
    (try
      (spit file (slurp (io/resource "version_template.txt")))
      (f)
      (finally
        (.delete file)))))
