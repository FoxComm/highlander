
SUBDIRS = api-js ashes firebird green-river isaac phoenix-scala prov-shit integrations integration-tests fox-notifications
UPDATEDIRS = $(SUBDIRS:%=update-%)
BUILDDIRS = $(SUBDIRS:%=build-%)
TESTDIRS = $(SUBDIRS:%=test-%)

build: $(BUILDDIRS)
$(BUILDDIRS): REPO = $(@:build-%=%) 
$(BUILDDIRS): 
	$(MAKE) -C $(REPO) build

test: $(TESTDIRS)
$(TESTDIRS): REPO = $(@:test-%=%) 
$(TESTDIRS): 
	$(MAKE) -C $(REPO) test

update: $(UPDATEDIRS)
$(UPDATEDIRS): REPO = $(@:update-%=%) 
$(UPDATEDIRS): REPOGIT = $(addsuffix .git,$(REPO)) 
$(UPDATEDIRS): 
	echo git subtree pull --prefix $(REPO) git@github.com:FoxComm/$(REPOGIT) $(REPO)
 

.PHONY: update build $(UPDATEDIRS) $(SUBDIRS) $(BUILDDIRS)
