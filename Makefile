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
