(ns toyokumo.tools.release
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [malli.core :as m]
   [malli.error :as me]
   [malli.transform :as mt]
   [semver.core :as v]
   [toyokumo.tools.util.git :as git]
   [toyokumo.tools.util.version :as version]))

(def -option
  [:map {:closed true}
   [:version-file :string]
   [:main-branch [:string {:default "main"}]]
   [:develop-branch [:string {:default "develop"}]]])

(defn- stringify-map-vals [m]
  (reduce-kv #(assoc %1 %2 (str %3)) {} m))

(defn- validate [option]
  (let [option (stringify-map-vals option)
        option (m/encode -option option mt/default-value-transformer)]
    (when-let [err (some-> (m/explain -option option)
                           (me/humanize))]
      (throw (AssertionError.  (str err))))
    option))

(defn init
  [option]
  (let [s (slurp (io/resource "version_template.txt"))]
    (-> (validate option)
        (:version-file)
        (spit s))))

(defn print-version
  [option]
  (-> (validate option)
      (:version-file)
      (version/get-version)
      (println)))

(defn bump-patch-version
  [option]
  (let [{:keys [version-file]} (validate option)]
    (version/update-version-file!
     version-file
     #(v/transform v/increment-patch %))))

(defn bump-minor-version
  [option]
  (let [{:keys [version-file]} (validate option)]
    (version/update-version-file!
     version-file
     #(v/transform v/increment-minor %))))

(defn bump-major-version
  [option]
  (let [{:keys [version-file]} (validate option)]
    (version/update-version-file!
     version-file
     #(v/transform v/increment-major %))))

(defn add-snapshot
  [option]
  (let [{:keys [version-file]} (validate option)]
    (version/update-version-file!
     version-file
     #(if (str/ends-with? % "-SNAPSHOT")
        %
        (str % "-SNAPSHOT")))))

(defn delete-snapshot
  [option]
  (let [{:keys [version-file]} (validate option)]
    (version/update-version-file!
     version-file
     #(str/replace % "-SNAPSHOT" ""))))

(defn pre-prod-deploy
  "Task before deploying to the production.

  - Start with :main-branch
  - Delete '-SNAPSHOT' from the version string

  Only these operations since we will commit and create a new tag after the deployment is successful."
  [option]
  (git/assert-committed)
  (delete-snapshot option))

(defn post-prod-deploy
  "Task after deploying to the production.

  - Start with :main-branch
  - Delete '-SNAPSHOT' from the version string
  - Commit all changes
  - Create a new tag with current version string
  - Push to :main-branch
  - Fetch :develop-branch
  - Switch to :develop-branch
  - Rebase changes between :main-branch
  - Bump the patch version
  - Add '-SNAPSHOT' to the version string
  - Commit all changes
  - Push to :develop-branch"
  [option]
  (let [{:keys [version-file main-branch develop-branch]} (validate option)]
    (delete-snapshot option)

    (let [version (version/get-version version-file)]
      (git/commit-all-changed! (str "version " version " [skip ci]"))
      (git/tag-this-version! version))

    (git/push-to! main-branch)
    (git/push-all-tags!)
    (git/fetch! develop-branch)
    (git/switch-to! develop-branch)
    (git/rebase! main-branch)

    (bump-patch-version option)
    (add-snapshot option)
    (let [version (version/get-version version-file)]
      (git/commit-all-changed! (str "version " version " [skip ci]"))
      (git/push-to! develop-branch))))
