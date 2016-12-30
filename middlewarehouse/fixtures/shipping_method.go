package fixtures

import (
	"database/sql/driver"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func GetShippingMethod(id uint, carrierID uint, carrier *models.Carrier) *models.ShippingMethod {
	return &models.ShippingMethod{
		Base: gormfox.Base{
			ID: id,
		},
		CarrierID:    carrierID,
		Carrier:      *carrier,
		Name:         "UPS 2 day ground",
		Code:         "EXPRESS",
		ShippingType: 1,
		Price:        599,
		Scope:        "1",
	}
}

func ToShippingMethodPayload(shippingMethod *models.ShippingMethod) *payloads.ShippingMethod {
	scope := payloads.Scopable{shippingMethod.Scope}
	sm := &payloads.ShippingMethod{
		CarrierID: shippingMethod.CarrierID,
		Name:      shippingMethod.Name,
		Code:      shippingMethod.Code,
		Price: payloads.Money{
			Currency: "USD",
			Value:    shippingMethod.Price,
		},
		Scopable: scope,
	}

	if shippingMethod.ShippingType == models.ShippingTypeFlat {
		sm.ShippingType = "flat"
	} else {
		sm.ShippingType = "variable"
	}

	return sm
}

func GetShippingMethodColumns() []string {
	return []string{
		"id",
		"carrier_id",
		"name",
		"code",
		"shipping_type",
		"cost",
	}
}

func GetShippingMethodRow(shippingMethod *models.ShippingMethod) []driver.Value {
	return []driver.Value{
		shippingMethod.ID,
		shippingMethod.CarrierID,
		shippingMethod.Name,
		shippingMethod.Code,
		shippingMethod.ShippingType,
		shippingMethod.Price,
	}
}
