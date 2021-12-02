(ns toyokumo.tools.util.version-test
  (:require
   [clojure.test :as t]
   [toyokumo.tools.test-helper :as h]
   [toyokumo.tools.util.version :as sut]))

(t/use-fixtures :each h/test-version-file-fixture)

(t/deftest update-version-file!-success-test
  (t/is (= "0.0.1-SNAPSHOT" (sut/get-version h/test-version-file)))

  (t/testing "Update to specific version"
    (sut/update-version-file! h/test-version-file (constantly "9.9.9"))
    (t/is (= "9.9.9" (sut/get-version h/test-version-file))))

  (t/testing "Update to add postfix"
    (sut/update-version-file! h/test-version-file #(str % "-SNAPSHOT"))
    (t/is (= "9.9.9-SNAPSHOT" (sut/get-version h/test-version-file)))))

(t/deftest update-version-file!-fail-test
  (t/testing "Cannot be updated to non semantic version"
    (t/is (= "0.0.1-SNAPSHOT" (sut/get-version h/test-version-file)))
    (t/is (thrown? AssertionError
            (sut/update-version-file! h/test-version-file (constantly "INVALID"))))
    (t/is (= "0.0.1-SNAPSHOT" (sut/get-version h/test-version-file)))))
