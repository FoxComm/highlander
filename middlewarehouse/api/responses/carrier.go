package responses

import "github.com/FoxComm/highlander/middlewarehouse/models"

type Carrier struct {
	ID               uint   `json:"id"`
	Name             string `json:"name"`
	TrackingTemplate string `json:"trackingTemplate"`
	Scope            string `json:"scope"`
}

func NewCarrierFromModel(carrier *models.Carrier) *Carrier {
	return &Carrier{
		ID:               carrier.ID,
		Name:             carrier.Name,
		TrackingTemplate: carrier.TrackingTemplate,
		Scope:            carrier.Scope,
	}
}
