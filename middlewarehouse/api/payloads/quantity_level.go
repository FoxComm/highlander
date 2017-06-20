package payloads

// QuantityLevel is an optional numerical level.
type QuantityLevel struct {
	IsEnabled bool `json:"isEnabled"`
	Level     int  `json:"level"`
}
