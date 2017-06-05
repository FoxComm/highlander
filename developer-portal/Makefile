include ../makelib
header = $(call baseheader, $(1), developer-portal)

SUBDIRS := content
BUILDDIRS := $(SUBDIRS:%=build-%)

clean:
	rm content/*.html
	rm content/**/*.html

prep:
	npm install
	cd node_modules/aglio-theme-fox && npm install

build:
	$(call header, Building)
	make prep
	make $(BUILDDIRS)

$(BUILDDIRS):
	$(MAKE) -C $(@:build-%=%) build

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	true

docker-push:
	$(call header, Registering)
	true

publish: build
	firebase use production
	firebase deploy

.PHONY: prep clean build test docker docker-push publish
