package responses

import (
	"github.com/FoxComm/middlewarehouse/models"
)

type StockLocation struct {
	ID      uint     `json:"id"`
	Name    string   `json:"name"`
	Type    string   `json:"type"`
	Address *Address `json:"address,omitempty"`
}

func NewStockLocationFromModel(location *models.StockLocation) *StockLocation {
	response := &StockLocation{
		ID:   location.ID,
		Name: location.Name,
		Type: location.Type,
	}

	if location.Address != nil {
		println("has addtress")
		response.Address = NewAddressFromModel(location.Address)
	}

	return response
}
