package payloads

type IncrementStockItemUnits struct {
	StockItemID uint
	Qty         uint
	UnitCost    uint
	Status      string
}
