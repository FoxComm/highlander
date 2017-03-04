package main

import "log"

const (
	hostname = "10.240.0.8:9200"
	mapping  = "products_search_view"
)

func main() {
	client := NewClient(hostname)
	if err := client.Connect(); err != nil {
		log.Fatal(err)
	}

	if err := client.UpdateMapping(mapping, true, true); err != nil {
		log.Fatal(err)
	}
}
