(ns toyokumo.tools.release-test
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :as t]
   [toyokumo.tools.release :as sut]
   [toyokumo.tools.test-helper :as h]
   [toyokumo.tools.util.git :as git]
   [toyokumo.tools.util.version :as version]))

(t/use-fixtures :each h/test-version-file-fixture)

(def ^:private test-option
  {:version-file h/test-version-file})

(defn- get-test-version []
  (version/get-version (:version-file test-option)))

(t/deftest init-test
  (let [file (io/file h/test-version-file)]
    (when (.exists file)
      (.delete file))

    (t/is (false? (.exists file)))
    (sut/init test-option)
    (t/is (true? (.exists file)))))

(t/deftest print-version-test
  (let [s (with-out-str
            (sut/print-version test-option))]
    (t/is (= "0.0.1-SNAPSHOT" (str/trim s)))))

(t/deftest bump-patch-version-test
  (t/is (= "0.0.1-SNAPSHOT" (get-test-version)))
  (sut/bump-patch-version test-option)
  (t/is (= "0.0.2-SNAPSHOT" (get-test-version))))

(t/deftest bump-minor-version-test
  (t/is (= "0.0.1-SNAPSHOT" (get-test-version)))
  (sut/bump-minor-version test-option)
  (t/is (= "0.1.1-SNAPSHOT" (get-test-version))))

(t/deftest bump-major-version-test
  (t/is (= "0.0.1-SNAPSHOT" (get-test-version)))
  (sut/bump-major-version test-option)
  (t/is (= "1.0.1-SNAPSHOT" (get-test-version))))

(t/deftest add-delete-snapshot-test
  (t/is (= "0.0.1-SNAPSHOT" (get-test-version)))

  (t/testing "delete"
    (sut/delete-snapshot test-option)
    (t/is (= "0.0.1" (get-test-version)))
    (sut/delete-snapshot test-option)
    (t/is (= "0.0.1" (get-test-version)))

    (t/testing "add"
      (sut/add-snapshot test-option)
      (t/is (= "0.0.1-SNAPSHOT" (get-test-version)))
      (sut/add-snapshot test-option)
      (t/is (= "0.0.1-SNAPSHOT" (get-test-version))))))

(t/deftest pre-prod-deploy-test
  (t/is (= "0.0.1-SNAPSHOT" (get-test-version)))
  (sut/pre-prod-deploy test-option)
  (t/is (= "0.0.1" (get-test-version))))

(t/deftest post-prod-deploy-default-branch-test
  (let [git-ops (atom [])]
    (with-redefs [git/sh+ (fn [& commands]
                            (swap! git-ops conj commands))]
      (t/testing "default main/develop branch"
        (t/is (= "0.0.1-SNAPSHOT" (get-test-version)))
        (sut/post-prod-deploy test-option)
        (t/is (= "0.0.2-SNAPSHOT" (get-test-version)))
        (t/is (= [["git" "commit" "-a" "-m" "version 0.0.1 [skip ci]"]
                  ["git" "tag" "-a" "v0.0.1" "-m" "version 0.0.1"]
                  ["git" "push" "origin" "main"]
                  ["git" "push" "--tags" "origin"]
                  ["git" "fetch" "origin" "develop"]
                  ["git" "switch" "develop"]
                  ["git" "rebase" "main"]
                  ["git" "commit" "-a" "-m" "version 0.0.2-SNAPSHOT [skip ci]"]
                  ["git" "push" "origin" "develop"]]
                 @git-ops))))))

(t/deftest post-prod-deploy-custom-branch-test
  (let [git-ops (atom [])
        custom-option (assoc test-option
                             :main-branch "master"
                             :develop-branch "dev")]
    (with-redefs [git/sh+ (fn [& commands]
                            (swap! git-ops conj commands))]
      (t/testing "default main/develop branch"
        (t/is (= "0.0.1-SNAPSHOT" (get-test-version)))
        (sut/post-prod-deploy custom-option)
        (t/is (= "0.0.2-SNAPSHOT" (get-test-version)))
        (t/is (= [["git" "commit" "-a" "-m" "version 0.0.1 [skip ci]"]
                  ["git" "tag" "-a" "v0.0.1" "-m" "version 0.0.1"]
                  ["git" "push" "origin" "master"]
                  ["git" "push" "--tags" "origin"]
                  ["git" "fetch" "origin" "dev"]
                  ["git" "switch" "dev"]
                  ["git" "rebase" "master"]
                  ["git" "commit" "-a" "-m" "version 0.0.2-SNAPSHOT [skip ci]"]
                  ["git" "push" "origin" "dev"]]
                 @git-ops))))))

(t/deftest post-prod-deploy-custom-tag-prefix-test
  (let [git-ops (atom [])]
    (with-redefs [git/sh+ (fn [& commands]
                            (swap! git-ops conj commands))]
      (t/is (= "0.0.1-SNAPSHOT" (get-test-version)))

      (sut/post-prod-deploy (assoc test-option :tag-prefix ""))
      (contains? (set @git-ops) ["git" "tag" "-a" "0.0.1" "-m" "version 0.0.1"])
      (t/is (= "0.0.2-SNAPSHOT" (get-test-version)))

      (sut/post-prod-deploy (assoc test-option :tag-prefix "ver"))
      (contains? (set @git-ops) ["git" "tag" "-a" "ver0.0.2" "-m" "version 0.0.2"])
      (t/is (= "0.0.3-SNAPSHOT" (get-test-version))))))
