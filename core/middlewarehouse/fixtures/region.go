package fixtures

import (
	"database/sql/driver"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func GetRegion(id uint, countryID uint, country *models.Country) *models.Region {
	return &models.Region{
		ID:        id,
		Name:      "Moscow",
		CountryID: countryID,
		Country:   *country,
	}
}

func ToRegionPayload(region *models.Region) *payloads.Region {
	return &payloads.Region{
		ID:          region.ID,
		Name:        region.Name,
		CountryID:   region.Country.ID,
		CountryName: region.Country.Name,
	}
}

func GetRegionColumns() []string {
	return []string{"id", "country_id", "name"}
}

func GetRegionRow(region *models.Region) []driver.Value {
	return []driver.Value{region.ID, region.CountryID, region.Name}
}
