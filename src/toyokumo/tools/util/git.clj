(ns toyokumo.tools.util.git
  (:require
   [clojure.java.shell :as sh]))

(defn sh+
  "Run `sh` and output the result.
  TODO: Improve to check the exit code"
  [& commands]
  (-> (apply sh/sh commands)
      println))

(defn assert-committed
  "cf. https://github.com/technomancy/leiningen/blob/master/src/leiningen/vcs.clj"
  []
  (let [res (with-out-str
              (sh+ "git" "status"))]
    (when (re-find #"Changes (not staged for commit|to be committed)" res)
      (throw (AssertionError. "There are uncommitted changes")))))

(defn commit-all-changed!
  [commit-message]
  (sh+ "git" "commit" "-a" "-m" commit-message))

(defn tag-this-version!
  [version-str]
  (sh+ "git" "tag" "-a" (str "v" version-str) "-m" (str "version " version-str)))

(defn push-to!
  [branch-name]
  (sh+ "git" "push" "origin" branch-name))

(defn push-all-tags!
  []
  (sh+ "git" "push" "--tags" "origin"))

(defn fetch! [branch-name]
  (sh+ "git" "fetch" "origin" branch-name))

(defn switch-to!
  [branch-name]
  (sh+ "git" "switch" branch-name))

(defn rebase!
  [branch-name]
  (sh+ "git" "rebase" branch-name))
