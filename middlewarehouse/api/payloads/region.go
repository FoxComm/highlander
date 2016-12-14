package payloads

import "github.com/FoxComm/highlander/middlewarehouse/models"

type Region struct {
	ID          uint   `json:"id"`
	Name        string `json:"name" binding:"required"`
	CountryID   uint   `json:"countryId" binding:"required"`
	CountryName string `json:"countryName"`
}

func (region Region) Model() *models.Region {
	return &models.Region{
		ID:        region.ID,
		Name:      region.Name,
		CountryID: region.CountryID,
		Country: models.Country{
			ID:   region.CountryID,
			Name: region.CountryName,
		},
	}
}
