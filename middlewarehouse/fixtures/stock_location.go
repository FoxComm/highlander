package fixtures

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func GetStockLocation() *models.StockLocation {
	return &models.StockLocation{
		Type:  "Warehouse",
		Name:  "TEST-LOCATION",
		Scope: "1",
	}
}
