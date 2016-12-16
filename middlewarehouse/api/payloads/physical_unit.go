package payloads

// PhysicalUnit is the representation of physical unit, specifically the
// value and the units that the value exists in.
type PhysicalUnit struct {
	Value float64 `json:"value"`
	Units string  `json:"units"`
}
