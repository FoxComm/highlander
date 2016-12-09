package payloads

import "errors"

type DecrementStockItemUnits struct {
	Qty  int    `json:"qty" binding:"required"`
	Type string `json:"type" binding:"required"`
	Scopable
}

func (r DecrementStockItemUnits) Validate() error {
	if r.Qty <= 0 {
		return errors.New("Qty must be greater than 0")
	}

	return nil
}
