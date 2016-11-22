package payloads

import (
	"errors"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

type Reservation struct {
	RefNum string            `json:"refNum" binding:"required"`
	Items  []ItemReservation `json:"items" binding:"required"`
}

type ItemReservation struct {
	SKU string `json:"sku" binding:"required"`
	Qty uint   `json:"qty" binding:"required"`
}

func (r Reservation) Validate() exceptions.IException {
	if len(r.Items) == 0 {
		return exceptions.NewValidationException(errors.New("Reservation must have at least one SKU"))
	}

	return nil
}
