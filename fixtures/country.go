package fixtures

import (
	"database/sql/driver"

	"github.com/FoxComm/middlewarehouse/models"
)

func GetCountry(id uint) *models.Country {
	return &models.Country{
		ID:               id,
		Name:             "Russia",
	}
}

func GetCountryColumns() []string {
	return []string{"id", "name"}
}

func GetCountryRow(country *models.Country) []driver.Value {
	return []driver.Value{country.ID, country.Name}
}
