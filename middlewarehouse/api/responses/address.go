package responses

import "github.com/FoxComm/highlander/middlewarehouse/models"

type Address struct {
	ID          uint   `json:"id"`
	Name        string `json:"name"`
	Region      Region `json:"region"`
	City        string `json:"city"`
	Zip         string `json:"zip"`
	Address1    string `json:"address1"`
	Address2    string `json:"address2"`
	PhoneNumber string `json:"phoneNumber"`
}

func NewAddressFromModel(model *models.Address) *Address {
	address := new(Address)

	address.ID = model.ID
	address.Name = model.Name
	address.City = model.City
	address.Zip = model.Zip
	address.Address1 = model.Address1
	if model.Address2.Valid {
		address.Address2 = model.Address2.String
	}
	address.PhoneNumber = model.PhoneNumber

	return address
}
