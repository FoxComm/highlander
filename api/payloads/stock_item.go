package payloads

type StockItem struct {
	SKU             string `json:"sku" binding:"required"`
	StockLocationID uint   `json:"stockLocationId" binding:"required"`
	DefaultUnitCost uint   `json:"defaultUnitCost"`
}
