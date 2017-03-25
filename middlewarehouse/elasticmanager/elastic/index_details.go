package elastic

// IndexDetails is a structure that matches how ElasticSearch describes the
// properties of an index.
type IndexDetails struct {
	Aliases  map[string]Alias   `json:"aliases"`
	Mappings map[string]Mapping `json:"mappings"`
}
