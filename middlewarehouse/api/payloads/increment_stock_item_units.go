package payloads

import (
	"errors"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

type IncrementStockItemUnits struct {
	Qty      int    `json:"qty" binding:"required"`
	UnitCost int    `json:"unitCost"`
	Status   string `json:"status"`
	Type     string `json:"type" binding:"required"`
}

func (r IncrementStockItemUnits) Validate() exceptions.IException {
	if r.Qty <= 0 {
		return exceptions.NewValidationException(errors.New("Qty must be greater than 0"))
	}

	return nil
}
