package responses

import "github.com/FoxComm/highlander/middlewarehouse/models"

type ShipmentLineItem struct {
	ID              uint   `json:"id"`
	ReferenceNumber string `json:"referenceNumber"`
	SkuId           uint   `json:"skuId"`
	SkuCode         string `json:"skuCode"`
	Name            string `json:"name"`
	Price           uint   `json:"price"`
	ImagePath       string `json:"imagePath"`
}

func NewShipmentLineItemFromModel(model *models.ShipmentLineItem) *ShipmentLineItem {
	return &ShipmentLineItem{
		ID:              model.ID,
		ReferenceNumber: model.ReferenceNumber,
		SkuId:           model.SkuId,
		SkuCode:         model.SkuCode,
		Name:            model.Name,
		Price:           model.Price,
		ImagePath:       model.ImagePath,
	}
}
