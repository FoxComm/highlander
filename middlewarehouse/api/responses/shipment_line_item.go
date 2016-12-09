package responses

import "github.com/FoxComm/highlander/middlewarehouse/models"

type ShipmentLineItem struct {
	ID               uint     `json:"id"`
	ReferenceNumbers []string `json:"referenceNumbers"`
	SKU              string   `json:"sku"`
	Name             string   `json:"name"`
	Price            uint     `json:"price"`
	ImagePath        string   `json:"imagePath"`
}

func NewShipmentLineItemFromModel(model *models.ShipmentLineItem) *ShipmentLineItem {
	return &ShipmentLineItem{
		ID:               model.ID,
		ReferenceNumbers: model.ReferenceNumbers,
		SKU:              model.SKU,
		Name:             model.Name,
		Price:            model.Price,
		ImagePath:        model.ImagePath,
	}
}
