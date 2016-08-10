package responses

import "github.com/FoxComm/middlewarehouse/models"

type Address struct {
	ID          uint    `json:"id"`
	Name        string  `json:"name"`
	Region      Region  `json:"region"`
	City        string  `json:"city"`
	Zip         string  `json:"zip"`
	Address1    string  `json:"address1"`
	Address2    *string `json:"address2"`
	PhoneNumber string  `json:"phoneNumber"`
}

func NewAddressFromModel(model *models.Address) *Address {
	return &Address{
		ID:          model.ID,
		Name:        model.Name,
		Region:      *NewRegionFromModel(&model.Region),
		City:        model.City,
		Zip:         model.Zip,
		Address1:    model.Address1,
		Address2:    NewStringFromSqlNullString(model.Address2),
		PhoneNumber: model.PhoneNumber,
	}
}
