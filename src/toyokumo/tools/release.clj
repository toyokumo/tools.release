(ns toyokumo.tools.release
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [pogonos.core :as pg]
   [semver.core :as v])
  (:import
   (java.time
    Instant)))

(def ^:private version-file
  ".version")

(def ^:private default-main-branch
  "main")

(def ^:private default-develop-branch
  "develop")

(defn- get-version []
  (let [version (str/trim (slurp version-file))]
    (assert (v/valid? version))
    version))

(defn- set-version! [version]
  (assert (v/valid? (str version)))
  (spit version-file (str version)))

(defn- sh+
  "shを実行して結果を出力する
  TODO 出力はexitコードをみるなど改善する"
  [& commands]
  (-> (apply sh/sh commands)
      println))

(defn- commit-all-changed
  []
  (let [version (get-version)]
    (sh+ "git" "commit" "-a" "-m" (str "version " version))))

(defn- tag-this-version
  []
  (let [version (get-version)]
    (sh+ "git" "tag" "-a" (str "v" version) "-m" (str "version " version))))

(defn- push-to
  [branch-name]
  (sh+ "git" "push" "origin" branch-name))

(defn- push-to-main
  []
  (sh+ "git" "push" "origin" "main"))

(defn- push-to-develop
  []
  (sh+ "git" "push" "origin" "develop"))

(defn- push-all-tags
  []
  (sh+ "git" "push" "--tags" "origin"))

(defn- fetch-develop
  []
  (sh+ "git" "fetch" "origin" "develop"))

(defn- switch-to-develop
  []
  (sh+ "git" "switch" "develop"))

(defn- rebase-main
  []
  (sh+ "git" "rebase" "main"))

;;; scripts

(defn print-version
  [& _]
  (println (get-version)))

(defn bump-patch-version
  [& _]
  (->> (get-version)
       (v/transform v/increment-patch)
       set-version!))

(defn bump-minor-version
  [& _]
  (->> (get-version)
       (v/transform v/increment-minor)
       set-version!))

(defn bump-major-version
  [& _]
  (->> (get-version)
       (v/transform v/increment-major)
       set-version!))

(defn add-snapshot
  [& _]
  (-> (get-version)
      (str "-SNAPSHOT")
      set-version!))

(defn delete-snapshot
  [& _]
  (-> (get-version)
      (str/replace "-SNAPSHOT" "")
      set-version!))

(defn generate-version-file
  [option]
  (assert (every? #(contains? option %) [:version-ns :version-file])
          ":ns-name and :output is required")
  (let [file (io/resource "version_template.mustache")
        content (pg/render-file file {:ns-name (:version-ns option)
                                      :version (get-version)
                                      :generated-at (str (Instant/now))})]
    (spit (str (:version-file option)) content)))

(defn pre-prod-deploy
  "本番環境へのデプロイ前のタスク
  - mainでスタート
  - バージョンから-SNAPSHOTを外す
  - バージョンを定数へと書き出し

  デプロイに成功してからコミットしたりタグを打ったりするのでデプロイ前はここまで"
  [option]
  (delete-snapshot)
  (generate-version-file option))

(defn post-prod-deploy
  "本番環境へのデプロイ後のタスク
  - mainでスタート
  - バージョンから-SNAPSHOTを外す
  - バージョンを定数へと書き出し
  - コミット
  - タグを打つ
  - プッシュ
  - origin/developをfetch
  - developにswitch
  - mainをrebase
  - パッチバージョンをあげる
  - -SNAPSHOTをつける
  - バージョンを定数へと書き出し
  - コミット
  - プッシュ"
  [option]
  (delete-snapshot)
  (generate-version-file option)
  (commit-all-changed)
  (tag-this-version)
  (push-to (:main-branch option default-main-branch))
  (push-all-tags)
  (fetch-develop)
  (switch-to-develop)
  (rebase-main)
  (bump-patch-version)
  (add-snapshot)
  (generate-version-file option)
  (commit-all-changed)
  (push-to (:develop-branch option default-develop-branch)))
