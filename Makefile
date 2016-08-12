
SUBDIRS = ashes firebird green-river isaac phoenix-scala prov-shit integrations integration-tests fox-notifications
UPDATEDIRS = $(SUBDIRS:%=update-%)
BUILDDIRS = $(SUBDIRS:%=build-%)
TESTDIRS = $(SUBDIRS:%=test-%)

build: $(BUILDDIRS)
    $(MAKE) -C api-js build
$(BUILDDIRS): REPO = $(@:build-%=%) 
$(BUILDDIRS): 
	$(MAKE) -C $(REPO) build

test: $(TESTDIRS)
    $(MAKE) -C api-js test
$(TESTDIRS): REPO = $(@:test-%=%) 
$(TESTDIRS): 
	$(MAKE) -C $(REPO) test

update: $(UPDATEDIRS)
	git subtree pull --prefix api-js git@github.com:FoxComm/api-js gh-pages
$(UPDATEDIRS): REPO = $(@:update-%=%) 
$(UPDATEDIRS): REPOGIT = $(addsuffix .git,$(REPO)) 
$(UPDATEDIRS): 
	git subtree pull --prefix $(REPO) git@github.com:FoxComm/$(REPOGIT) master
 

.PHONY: update build $(UPDATEDIRS) $(SUBDIRS) $(BUILDDIRS)
