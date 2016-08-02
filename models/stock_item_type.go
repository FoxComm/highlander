package models

type StockItemType struct {
	Sellable    int
	NonSellable int
	Backorder   int
	Preorder    int
}

var types *StockItemType

func initStockItemTypes() {
	types = &StockItemType{
		Sellable:    1,
		NonSellable: 2,
		Backorder:   3,
		Preorder:    4,
	}
}

func StockItemTypes() StockItemType {
	if types == nil {
		initStockItemTypes()
	}

	return *types
}
