package payloads

type StockItem struct {
	SkuID           uint   `json:"skuId" binding:"required"`
	SkuCode         string `json:"skuCode" binding:"required"`
	StockLocationID uint   `json:"stockLocationId" binding:"required"`
	DefaultUnitCost int    `json:"defaultUnitCost"`
}
