package payloads

import "errors"

type DecrementStockItemUnits struct {
	Qty int
}

func (r DecrementStockItemUnits) Validate() error {
	if r.Qty <= 0 {
		return errors.New("Qty must be greater than 0")
	}

	return nil
}
