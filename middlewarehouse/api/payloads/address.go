package payloads

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/db/utils"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

type Address struct {
	ID          uint    `json:"id"`
	Name        string  `json:"name"`
	Region      Region  `json:"region" binding:"required"`
	City        string  `json:"city" binding:"required"`
	Zip         string  `json:"zip" binding:"required"`
	Address1    string  `json:"address1" binding:"required"`
	Address2    *string `json:"address2"`
	PhoneNumber string  `json:"phoneNumber"`
}

func (address *Address) Model() *models.Address {
	return &models.Address{
		Name:        address.Name,
		RegionID:    address.Region.ID,
		Region:      *(address.Region.Model()),
		Address1:    address.Address1,
		City:        address.City,
		Zip:         address.Zip,
		Address2:    utils.MakeSqlNullString(address.Address2),
		PhoneNumber: address.PhoneNumber,
	}
}
