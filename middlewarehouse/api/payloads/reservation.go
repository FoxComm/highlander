package payloads

import "errors"

type Reservation struct {
	RefNum string            `json:"refNum" binding:"required"`
	Items  []itemReservation `json:"items" binding:"required"`
	Scopable
}

type itemReservation struct {
	SKU string `json:"sku" binding:"required"`
	Qty uint   `json:"qty" binding:"required"`
}

func (r Reservation) Validate() error {
	if len(r.Items) == 0 {
		return errors.New("Reservation must have at least one SKU")
	}

	return nil
}
