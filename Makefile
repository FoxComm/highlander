SUBDIRS = $(shell ./projects.sh)
$(info $(SUBDIRS))
UPDATEDIRS = $(SUBDIRS:%=update-%)
BUILDDIRS = $(SUBDIRS:%=build-%)
TESTDIRS = $(SUBDIRS:%=test-%)
CLEANDIRS = $(SUBDIRS:%=clean-%)
DOCKERDIRS = $(SUBDIRS:%=docker-%)
DOCKERPUSHDIRS = $(SUBDIRS:%=docker-push-%)

SUBDIRS_ALL = $(shell ./projects.sh -all)
$(info $(SUBDIRS_ALL))
BUILDDIRS_ALL = $(SUBDIRS_ALL:%=build-all-%)
TESTDIRS_ALL = $(SUBDIRS_ALL:%=test-all-%)
DOCKERDIRS_ALL = $(SUBDIRS_ALL:%=docker-all-%)
DOCKERPUSHDIRS_ALL = $(SUBDIRS_ALL:%=docker-push-all-%)

clean: $(CLEANDIRS)
$(CLEANDIRS): REPO = $(@:clean-%=%)
$(CLEANDIRS):
	$(MAKE) -C $(REPO) clean

build: $(BUILDDIRS)
$(BUILDDIRS): REPO = $(@:build-%=%)
$(BUILDDIRS):
	$(MAKE) -C $(REPO) build

build-all: $(BUILDDIRS_ALL)
$(BUILDDIRS_ALL): REPO = $(@:build-all-%=%)
$(BUILDDIRS_ALL):
	$(MAKE) -C $(REPO) build

test: $(TESTDIRS)

$(TESTDIRS): REPO = $(@:test-%=%)
$(TESTDIRS):
	$(MAKE) -C $(REPO) test

test-all: $(TESTDIRS_ALL)

$(TESTDIRS_ALL): REPO = $(@:test-all-%=%)
$(TESTDIRS_ALL):
	$(MAKE) -C $(REPO) test

update: $(UPDATEDIRS)
	git subtree pull --prefix api-js git@github.com:FoxComm/api-js gh-pages
$(UPDATEDIRS): REPO = $(@:update-%=%)
$(UPDATEDIRS): REPOGIT = $(addsuffix .git,$(REPO))
$(UPDATEDIRS):
	git subtree pull --prefix $(REPO) git@github.com:FoxComm/$(REPOGIT) master

docker: $(DOCKERDIRS)
$(DOCKERDIRS): REPO = $(@:docker-%=%)
$(DOCKERDIRS):
	$(MAKE) -C $(REPO) docker

docker-push: $(DOCKERPUSHDIRS)
$(DOCKERPUSHDIRS): REPO = $(@:docker-%=%)
$(DOCKERPUSHDIRS):
	$(MAKE) -C $(REPO) docker-push

docker-all: $(DOCKERDIRS_ALL)
$(DOCKERDIRS_ALL): REPO = $(@:docker-all-%=%)
$(DOCKERDIRS_ALL):
	$(MAKE) -C $(REPO) docker

docker-push-all: $(DOCKERPUSHDIRS_ALL)
$(DOCKERPUSHDIRS_ALL): REPO = $(@:docker-push-all-%=%)
$(DOCKERPUSHDIRS_ALL):
	$(MAKE) -C $(REPO) docker-push

.PHONY: update build $(UPDATEDIRS) $(SUBDIRS) $(BUILDDIRS)
