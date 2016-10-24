package fixtures

import (
	"database/sql/driver"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func GetShippingMethod(id uint, carrierID uint, carrier *models.Carrier) *models.ShippingMethod {
	return &models.ShippingMethod{
		ID:        id,
		CarrierID: carrierID,
		Carrier:   *carrier,
		Name:      "UPS 2 day ground",
		Code:      "EXPRESS",
		Scope:     "1",
	}
}

func ToShippingMethodPayload(shippingMethod *models.ShippingMethod) *payloads.ShippingMethod {
	payload := &payloads.ShippingMethod{
		CarrierID: shippingMethod.CarrierID,
		Name:      shippingMethod.Name,
		Code:      shippingMethod.Code,
	}

	payload.Scope = shippingMethod.Scope

	return payload
}

func GetShippingMethodColumns() []string {
	return []string{"id", "carrier_id", "name", "code"}
}

func GetShippingMethodRow(shippingMethod *models.ShippingMethod) []driver.Value {
	return []driver.Value{shippingMethod.ID, shippingMethod.CarrierID, shippingMethod.Name, shippingMethod.Code}
}
