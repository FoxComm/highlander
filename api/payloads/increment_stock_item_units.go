package payloads

import "errors"

type IncrementStockItemUnits struct {
	Qty      int
	UnitCost int
	Status   string
}

func (r IncrementStockItemUnits) Validate() error {
	if r.Qty <= 0 {
		return errors.New("Qty must be greater than 0")
	}

	return nil
}