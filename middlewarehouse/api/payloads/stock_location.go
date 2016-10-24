package payloads

type StockLocation struct {
	Name    string   `json:"name" binding:"required"`
	Type    string   `json:"type" binding:"required"`
	Address *Address `json:"address"`
	Scopable
}

func (stockLocation *StockLocation) SetScope(scope string) {
	stockLocation.Scope = scope

	if stockLocation.Address != nil {
		stockLocation.Address.SetScope(scope)
	}
}
