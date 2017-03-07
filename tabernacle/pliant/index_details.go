package main

// IndexDetails is a structure that matches how ElasticSearch describes the
// properties of an index.
type IndexDetails struct {
	Aliases  map[string]interface{} `json:"aliases"`
	Mappings map[string]interface{} `json:"mappings"`
}
