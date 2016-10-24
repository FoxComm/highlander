package responses

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

type StockLocation struct {
	ID      uint     `json:"id"`
	Name    string   `json:"name"`
	Type    string   `json:"type"`
	Address *Address `json:"address,omitempty"`
	Scope   string   `json:"scope"`
}

func NewStockLocationsFromModels(locations []*models.StockLocation) []*StockLocation {
	response := make([]*StockLocation, len(locations))
	for i := range locations {
		response[i] = NewStockLocationFromModel(locations[i])
	}

	return response
}

func NewStockLocationFromModel(location *models.StockLocation) *StockLocation {
	response := &StockLocation{
		ID:   location.ID,
		Name: location.Name,
		Type: location.Type,
	}

	if location.Address != nil {
		response.Address = NewAddressFromModel(location.Address)
	}

	response.Scope = location.Scope

	return response
}
