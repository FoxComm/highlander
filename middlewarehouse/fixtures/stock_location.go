package fixtures

import (
	"github.com/FoxComm/middlewarehouse/models"
)

func GetStockLocation() *models.StockLocation {
	return &models.StockLocation{
		Type: "Warehouse",
		Name: "TEST-LOCATION",
	}
}
