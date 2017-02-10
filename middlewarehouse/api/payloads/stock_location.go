package payloads

import "github.com/FoxComm/highlander/middlewarehouse/models"

type StockLocation struct {
	Name    string   `json:"name" binding:"required"`
	Type    string   `json:"type" binding:"required"`
	Address *Address `json:"address"`
	Scopable
}

func (payload *StockLocation) Model() *models.StockLocation {
	var address *models.Address
	if payload.Address != nil {
		address = payload.Address.Model()
	}

	location := &models.StockLocation{
		Name:    payload.Name,
		Type:    payload.Type,
		Address: address,
		Scope:   payload.Scope,
	}

	return location
}
