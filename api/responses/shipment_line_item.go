package responses

import "github.com/FoxComm/middlewarehouse/models"

type ShipmentLineItem struct {
	ID              uint   `json:"id"`
	SKU             string `json:"sku"`
	Name            string `json:"name"`
	Price           uint   `json:"price"`
	ImagePath       string `json:"imagePath"`
}

func NewShipmentLineItemFromModel(model *models.ShipmentLineItem) *ShipmentLineItem {
	return &ShipmentLineItem{
		ID:              model.ID,
		SKU:             model.SKU,
		Name:            model.Name,
		Price:           model.Price,
		ImagePath:       model.ImagePath,
	}
}
