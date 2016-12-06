package payloads

type StockLocation struct {
	Name    string   `json:"name" binding:"required"`
	Type    string   `json:"type" binding:"required"`
	Address *Address `json:"address"`
	Scopable
}
