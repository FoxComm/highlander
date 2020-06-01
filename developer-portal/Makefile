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
	rsync -av node_modules/aglio-theme-fox/templates/assets/ content/assets/

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

serve: build
	firebase serve

.PHONY: prep clean build test docker docker-push publish
