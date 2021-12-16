VERSION_FILE := src/toyokumo/tools/release/version.clj
RELEASE_OPTION := :version-file ${VERSION_FILE}

.PHONY: lint
lint:
	cljstyle check
	clj-kondo --lint src:test

.PHONY: test
test:
	clojure -M:dev:test

.PHONY: outdated
outdated:
	clojure -Sdeps '{:deps {com.github.liquidz/antq {:mvn/version "RELEASE"}}}' -M -m antq.core

.PHONY: bump-minor-version
bump-minor-version:
	clojure -X toyokumo.tools.release/bump-minor-version ${RELEASE_OPTION}

.PHONY: bump-major-version
bump-major-version:
	clojure -X toyokumo.tools.release/bump-major-version ${RELEASE_OPTION}

.PHONY: release
release:
	git checkout main
	git pull
	clojure -X toyokumo.tools.release/pre-prod-deploy ${RELEASE_OPTION}
	clojure -X toyokumo.tools.release/post-prod-deploy ${RELEASE_OPTION}
