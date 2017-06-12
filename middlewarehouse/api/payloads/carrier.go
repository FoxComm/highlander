package payloads

import "github.com/FoxComm/highlander/middlewarehouse/models"

type Carrier struct {
	Name             string `json:"name" binding:"required"`
	TrackingTemplate string `json:"trackingTemplate" binding:"required"`
	Scopable
}

func (carrier Carrier) Model() *models.Carrier {
	return &models.Carrier{
		Name:             carrier.Name,
		TrackingTemplate: carrier.TrackingTemplate,
		Scope:            carrier.Scope,
	}
}
