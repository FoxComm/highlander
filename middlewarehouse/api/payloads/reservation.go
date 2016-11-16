package payloads

import "errors"

type Reservation struct {
	RefNum string            `json:"refNum" binding:"required"`
	Items  []ItemReservation `json:"items" binding:"required"`
}

type ItemReservation struct {
	SkuID uint `json:"skuId" binding:"required"`
	Qty   uint `json:"qty" binding:"required"`
}

func (r Reservation) Validate() error {
	if len(r.Items) == 0 {
		return errors.New("Reservation must have at least one SKU")
	}

	return nil
}
