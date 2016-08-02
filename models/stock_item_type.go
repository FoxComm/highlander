package models

import (
	"fmt"
)

type itemType struct {
	Sellable    uint
	NonSellable uint
	Backorder   uint
	Preorder    uint
}

var types *itemType

func initStockItemTypes() {
	types = &itemType{
		Sellable:    1,
		NonSellable: 2,
		Backorder:   3,
		Preorder:    4,
	}
}

func StockItemTypes() itemType {
	if types == nil {
		initStockItemTypes()
	}

	return *types
}

func ValidStockItemTypes() []string {
	return []string{"sellable", "non-sellable", "backorder", "preorder"}
}

func StockItemTypeFromString(strType string) (uint, error) {
	types := StockItemTypes()

	switch strType {
	case "sellable":
		return types.Sellable, nil
	case "non-sellable":
		return types.NonSellable, nil
	case "backorder":
		return types.Backorder, nil
	case "preorder":
		return types.Preorder, nil
	}

	return 0, fmt.Errorf(`Wrong stock item type "%s"`, strType)
}
