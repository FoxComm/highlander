package models

import (
	"fmt"
)

type itemType struct {
}

const (
	Sellable    uint = 1
	NonSellable uint = 2
	Backorder   uint = 3
	Preorder    uint = 4
)

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
	switch strType {
	case "Sellable":
		return Sellable
	case "Non-sellable":
		return NonSellable
	case "Backorder":
		return Backorder
	case "Preorder":
		return Preorder
	default:
		return Sellable
	}
}
