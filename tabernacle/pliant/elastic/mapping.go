package elastic

// Mappings is a map containing a collection of mappings. The keys are names of
// the mappings.
type Mappings map[string]Mapping

// Mapping is a data structure representing an ElasticSearch mapping.
type Mapping struct {
	Properties map[string]interface{} `json:"properties"`
}
