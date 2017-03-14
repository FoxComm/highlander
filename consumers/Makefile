SUBDIRS = product-index
BUILDDIRS = $(SUBDIRS:%=build-%)
TESTDIRS = $(SUBDIRS:%=test-%)
DOCKERDIRS = $(SUBDIRS:%=docker-%)
DOCKERPUSHDIRS = $(SUBDIRS:%=docker-push-%)

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

.PHONY: build
