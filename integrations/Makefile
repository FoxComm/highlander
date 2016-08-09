SUBDIRS = shipstation
BUILDDIRS = $(SUBDIRS:%=build-%)

build:
$(BUILDDIRS): REPO = $(@:build-%=%) 
$(BUILDDIRS): 
	$(MAKE) -C $(REPO) build

.PHONY: build
