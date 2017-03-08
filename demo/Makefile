SUBDIRS := peacock
BUILDDIRS := $(SUBDIRS:%=build-%)
CLEANDIRS := $(SUBDIRS:%=clean-%)
TESTDIRS := $(SUBDIRS:%=test-%)
DOCKERDIRS := $(SUBDIRS:%=docker-%)
DOCKERPUSHDIRS := $(SUBDIRS:%=docker-push-%)

clean: $(CLEANDIRS)
$(CLEANDIRS):
	$(MAKE) -C $(@:clean-%=%) clean

build: $(BUILDDIRS)
$(BUILDDIRS):
	$(MAKE) -C $(@:build-%=%) build

test: $(TESTDIRS)
$(TESTDIRS):
	$(MAKE) -C $(@:test-%=%) test

docker: $(DOCKERDIRS)
$(DOCKERDIRS):
	$(MAKE) -C $(@:docker-%=%) docker

docker-push: $(DOCKERPUSHDIRS)
$(DOCKERPUSHDIRS):
	$(MAKE) -C $(@:docker-push-%=%) docker-push

.PHONY: clean build test docker docker-push
