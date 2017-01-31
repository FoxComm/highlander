package payloads

import (
	"encoding/json"
)

type CustomerGroupPayload struct {
	Name           string          `json:"name"`
	GroupType      string          `json:"groupType"`
	CustomersCount int             `json:"customersCount"`
	ClientState    json.RawMessage `json:"clientState"`
	ElasticRequest json.RawMessage `json:"elasticRequest"`
}
