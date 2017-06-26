package responses

type Neo4jError struct {
	Code    string `json:"code"`
	Message string `json:"message"`
}

type Neo4jResultsDataGraphNodesProperties struct {
	PhoenixID int    `json:"phoenix_id"`
	SKU       string `json:"sku"`
}

type Neo4jResultsDataGraphNodes struct {
	ID         string                               `json:"id"`
	Labels     []string                             `json:"labels"`
	Properties Neo4jResultsDataGraphNodesProperties `json:"properties"`
}

type Neo4jResultsDataGraph struct {
	Nodes         []Neo4jResultsDataGraphNodes `json:"nodes"`
	Relationships []interface{}                `json:"relationships"`
}

type Neo4jResultsDataMeta struct {
	ID      int    `json:"id"`
	Type    string `json:"type"`
	Deleted bool   `json:"deleted"`
}

type Neo4jResultsDataRow struct {
	PhoenixID int    `json:"phoenix_id"`
	SKU       string `json:"sku"`
}

type Neo4jResultsData struct {
	Row   []Neo4jResultsDataRow  `json:"row"`
	Meta  []Neo4jResultsDataMeta `json:"meta"`
	Graph Neo4jResultsDataGraph  `json:"graph"`
}

type Neo4jResults struct {
	Columns []string           `json:"columns"`
	Data    []Neo4jResultsData `json:"data"`
}

type Neo4jResponse struct {
	Results []Neo4jResults `json:"results"`
	Errors  []Neo4jError   `json:"errors"`
}
