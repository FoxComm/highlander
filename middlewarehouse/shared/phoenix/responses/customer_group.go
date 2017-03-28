package responses

import "encoding/json"

type CustomerGroupResponse struct {
	ID             int             `json:"id"`
	Name           string          `json:"name"`
	GroupType      string          `json:"groupType"`
	CustomersCount int             `json:"customersCount"`
	ClientState    json.RawMessage `json:"clientState"`
	ElasticRequest json.RawMessage `json:"elasticRequest"`
}
