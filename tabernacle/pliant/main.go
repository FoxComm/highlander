package main

import (
	"log"

	"github.com/FoxComm/highlander/tabernacle/pliant/cmd"
)

const (
	index    = "admin"
	hostname = "10.240.0.6:9200"
	mapping  = "products_search_view"

	defaultConfig = "config.yml"
)

func main() {
	// client := elastic.NewClient(hostname)
	// if err := client.Connect(); err != nil {
	// 	log.Fatal(err)
	// }

	// details, err := client.GetAllMappings()
	// if err != nil {
	// 	log.Fatal(err)
	// }

	// for _, indexDetails := range details {
	// 	for mappingName, mappingContents := range indexDetails.Mappings {
	// 		filename := fmt.Sprintf("./mappings/%s.json", mappingName)
	// 		log.Printf("Writing file %s", filename)

	// 		contents, err := json.Marshal(mappingContents)
	// 		if err != nil {
	// 			log.Fatal(err)
	// 		}

	// 		if err := ioutil.WriteFile(filename, contents, 0644); err != nil {
	// 			log.Fatal(err)
	// 		}
	// 	}
	// }

	cfg, err := cmd.NewConfig(defaultConfig)
	if err != nil {
		log.Fatal(err)
	}

	log.Printf("The ES URL is: %s", cfg.ElasticURL())

	for _, alias := range cfg.ElasticAliases() {
		log.Printf("Alias Name: %s", alias.Name)
	}

	for n, c := range cfg.DatabaseConnectionStrings() {
		log.Printf("URL for %s is %s", n, c)
	}
}
