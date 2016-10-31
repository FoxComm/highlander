package payloads

import "errors"

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
