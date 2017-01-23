package responses

import "encoding/json"

type CustomerGroupResponse struct {
	ID             int
	Name           string
	Type           string
	ElasticRequest json.RawMessage
}
