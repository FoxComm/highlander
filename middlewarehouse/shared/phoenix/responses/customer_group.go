package responses

import "encoding/json"

type CustomerGroupResponse struct {
	ID             int
	Name           string
	Type           string
	CustomersCount int
	ClientState    json.RawMessage
	ElasticRequest json.RawMessage
}
