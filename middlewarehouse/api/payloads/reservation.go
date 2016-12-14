package payloads

import (
	"errors"

	"github.com/FoxComm/highlander/middlewarehouse/models"
)

type Reservation struct {
	RefNum string            `json:"refNum" binding:"required"`
	Items  []ItemReservation `json:"items" binding:"required"`
	Scopable
}

//TODO Do we really need this? Looks like it's not used at all
type ItemReservation struct {
	SKU string `json:"sku" binding:"required"`
	Qty uint   `json:"qty" binding:"required"`
}

func (r Reservation) Model() *models.Reservation {
	return &models.Reservation{
		RefNum: r.RefNum,
		Scope:  r.Scope,
	}
}

func (r Reservation) Validate() error {
	if len(r.Items) == 0 {
		return errors.New("Reservation must have at least one SKU")
	}

	return nil
}
