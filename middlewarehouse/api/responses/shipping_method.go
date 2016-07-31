package responses

import "github.com/FoxComm/highlander/middlewarehouse/models"

type ShippingMethod struct {
	ID        uint   `json:"id"`
	CarrierID uint   `json:"carrierId"`
	Name      string `json:"name"`
}

func NewShippingMethodFromModel(shippingMethod *models.ShippingMethod) *ShippingMethod {
	return &ShippingMethod{
		ID:        shippingMethod.ID,
		CarrierID: shippingMethod.CarrierID,
		Name:      shippingMethod.Name,
	}
}
