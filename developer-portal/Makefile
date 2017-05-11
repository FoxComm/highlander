SUBDIRS := content
BUILDDIRS := $(SUBDIRS:%=build-%)

clean: 
	rm content/*.html
	rm content/**/*.html

prep:
	npm install

build: prep $(BUILDDIRS)

$(BUILDDIRS):
	$(MAKE) -C $(@:build-%=%) build

test:
	true

docker: 
	true

docker-push:
	true

.PHONY: prep clean build test docker docker-push
