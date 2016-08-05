package responses

import "github.com/FoxComm/middlewarehouse/models"

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
		CountryID:   model.CountryID,
		CountryName: model.CountryName,
	}
}
