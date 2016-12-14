package payloads

import (
	"errors"

	"github.com/FoxComm/highlander/middlewarehouse/models"
)

type IncrementStockItemUnits struct {
	Qty      int    `json:"qty" binding:"required"`
	UnitCost int    `json:"unitCost"`
	Status   string `json:"status"`
	Type     string `json:"type" binding:"required"`
	Scopable
}

func (r IncrementStockItemUnits) Validate() error {
	if r.Qty <= 0 {
		return errors.New("Qty must be greater than 0")
	}

	return nil
}

func (r IncrementStockItemUnits) Models(stockItemID uint) []*models.StockItemUnit {
	units := []*models.StockItemUnit{}

	for i := 0; i < r.Qty; i++ {
		item := &models.StockItemUnit{
			StockItemID: stockItemID,
			UnitCost:    r.UnitCost,
			Status:      models.UnitStatus(r.Status),
			Type:        models.UnitType(r.Type),
		}
		units = append(units, item)
	}

	return units
}
