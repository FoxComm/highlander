MD = $(wildcard *.md **/*.md)
HTML = $(patsubst %.md,%.html,$(MD))

%.html: %.md
	../node_modules/.bin/aglio -t fox --theme-template triple -i $< -o $@ \

build: $(HTML)

.PHONY: build
