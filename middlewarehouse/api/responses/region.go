package responses

import "github.com/FoxComm/highlander/middlewarehouse/models"

type Region struct {
	ID          uint   `json:"id"`
	Name        string `json:"name"`
	CountryID   uint   `json:"countryId"`
	CountryName string `json:"countryName"`
}

func NewRegionFromModel(model *models.Region) *Region {
	return &Region{
		ID:          model.ID,
		Name:        model.Name,
		CountryID:   model.Country.ID,
		CountryName: model.Country.Name,
	}
}
