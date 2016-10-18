package main

type UpdatePayload struct {
	Field   string                 `json:"field"`
	Value   string                 `json:"value"`
	Product map[string]interface{} `json:"product"`
}
