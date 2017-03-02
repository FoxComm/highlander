SUBDIRS := peacock
BUILDDIRS := $(SUBDIRS:%=build-%)
CLEANDIRS := $(SUBDIRS:%=clean-%)
TESTDIRS := $(SUBDIRS:%=test-%)
DOCKERDIRS := $(SUBDIRS:%=docker-%)
DOCKERPUSHDIRS := $(SUBDIRS:%=docker-push-%)

clean: $(CLEANDIRS)
$(CLEANDIRS):
	REPO = $(@:clean-%=%)
	$(MAKE) -C $(REPO) clean

build: $(BUILDDIRS)
$(BUILDDIRS):
	REPO = $(@:build-%=%)
	$(MAKE) -C $(REPO) build

test: $(TESTDIRS)
$(TESTDIRS):
	REPO = $(@:test-%=%)
	$(MAKE) -C $(REPO) test

docker: $(DOCKERDIRS)
$(DOCKERDIRS):
	REPO = $(@:docker-%=%)
	$(MAKE) -C $(REPO) docker

docker-push: $(DOCKERPUSHDIRS)
$(DOCKERPUSHDIRS):
	REPO = $(@:docker-push-%=%)
	$(MAKE) -C $(REPO) docker-push

.PHONY: clean build test docker docker-push
