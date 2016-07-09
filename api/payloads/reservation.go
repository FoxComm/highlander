package payloads

import "errors"

type Reservation struct {
	RefNum string           `json:"refNum" binding:"required"`
	SKUs   []SKUReservation `json:"skus" binding:"required"`
}

type SKUReservation struct {
	SKU string `json:"sku" binding:"required"`
	Qty uint   `json:"qty" binding:"required"`
}

func (r Reservation) Validate() error {
	if len(r.SKUs) == 0 {
		return errors.New("Reservation must have at least one SKU")
	}

	return nil
}
