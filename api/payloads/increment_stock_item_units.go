package payloads

import "errors"

type IncrementStockItemUnits struct {
	Qty      int `json:"qty" binding:"required"`
	UnitCost int
	Status   string
	Type     string `json:"type" binding:"required"`
}

func (r IncrementStockItemUnits) Validate() error {
	if r.Qty <= 0 {
		return errors.New("Qty must be greater than 0")
	}

	return nil
}
