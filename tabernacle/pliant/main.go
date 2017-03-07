package main

import "log"

const (
	index    = "admin"
	hostname = "10.240.0.6:9200"
	mapping  = "products_search_view"
)

func main() {
	client := NewClient(hostname)
	if err := client.Connect(); err != nil {
		log.Fatal(err)
	}

	details, err := client.GetMappings(index)
	if err != nil {
		log.Fatal(err)
	}

	for mappingName, _ := range details.Mappings {
		log.Printf("Mapping name is %s", mappingName)
	}
}
