package payloads

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

type ShipmentLineItem struct {
	ID               uint     `json:"id"`
	ReferenceNumbers []string `json:"referenceNumbers" binding:"required"`
	SKU              string   `json:"sku" binding:"required"`
	Name             string   `json:"name" binding:"required"`
	Price            uint     `json:"price" binding:"required"`
	ImagePath        string   `json:"imagePath" binding:"required"`
}

func (payload *ShipmentLineItem) Model() *models.ShipmentLineItem {
	return &models.ShipmentLineItem{
		Base: gormfox.Base{
			ID: payload.ID,
		},
		ReferenceNumbers: payload.ReferenceNumbers,
		SKU:              payload.SKU,
		Name:             payload.Name,
		Price:            payload.Price,
		ImagePath:        payload.ImagePath,
	}
}
