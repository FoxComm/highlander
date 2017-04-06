package payloads

// CreateContent is a representation of the payload that can create any generic
// content object.
type CreateContent struct {
	Attributes map[string]interface{} `json:"attributes"`
}
