package payloads

import (
	"errors"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

type DecrementStockItemUnits struct {
	Qty  int    `json:"qty" binding:"required"`
	Type string `json:"type" binding:"required"`
}

func (r DecrementStockItemUnits) Validate() exceptions.IException {
	if r.Qty <= 0 {
		return exceptions.NewValidationException(errors.New("Qty must be greater than 0"))
	}

	return nil
}
