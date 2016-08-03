package models

import "fmt"

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
	return []string{"Sellable", "Non-sellable", "Backorder", "Preorder"}
}

func ValidateStockItemType(strType string) error {
	for _, str := range ValidStockItemTypes() {
		if str == strType {
			return nil
		}
	}

	return fmt.Errorf(`Wrong stock item type "%s"`, strType)
}

func StockItemTypeToString(id uint) string {
	return ValidStockItemTypes()[id-1]
}

func StockItemTypeFromString(strType string) uint {
	types := StockItemTypes()

	switch strType {
	case "Sellable":
		return types.Sellable
	case "Non-sellable":
		return types.NonSellable
	case "Backorder":
		return types.Backorder
	case "Preorder":
		return types.Preorder
	default:
		return types.Sellable
	}
}
