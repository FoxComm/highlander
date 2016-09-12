package responses

import "github.com/FoxComm/middlewarehouse/models"

type ShippingMethod struct {
	ID      uint    `json:"id"`
	Carrier Carrier `json:"carrier"`
	Name    string  `json:"name"`
	Code    string  `json:"code"`
}

func NewShippingMethodFromModel(shippingMethod *models.ShippingMethod) *ShippingMethod {
	return &ShippingMethod{
		ID:      shippingMethod.ID,
		Carrier: *NewCarrierFromModel(&shippingMethod.Carrier),
		Name:    shippingMethod.Name,
		Code:    shippingMethod.Code,
	}
}
