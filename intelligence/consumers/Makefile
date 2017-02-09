SUBDIRS = digger-sphex orders-sphex
BUILDDIRS = $(SUBDIRS:%=build-%)
CLEANDIRS = $(SUBDIRS:%=clean-%)
TESTDIRS = $(SUBDIRS:%=test-%)
DOCKERDIRS = $(SUBDIRS:%=docker-%)
DOCKERPUSHDIRS = $(SUBDIRS:%=docker-push-%)

clean: $(CLEANDIRS)
$(CLEANDIRS): REPO = $(@:clean-%=%) 
$(CLEANDIRS): 
	$(MAKE) -C $(REPO) clean

build: $(BUILDDIRS)
$(BUILDDIRS): REPO = $(@:build-%=%) 
$(BUILDDIRS): 
	$(MAKE) -C $(REPO) build

test: $(TESTDIRS)
$(TESTDIRS): REPO = $(@:test-%=%) 
$(TESTDIRS): 
	$(MAKE) -C $(REPO) test

docker: $(DOCKERDIRS)
$(DOCKERDIRS): REPO = $(@:docker-%=%) 
$(DOCKERDIRS): 
	$(MAKE) -C $(REPO) docker

docker-push: $(DOCKERPUSHDIRS)
$(DOCKERPUSHDIRS): REPO = $(@:docker-push-%=%) 
$(DOCKERPUSHDIRS): 
	$(MAKE) -C $(REPO) docker-push

.PHONY: clean build test docker docker-push
