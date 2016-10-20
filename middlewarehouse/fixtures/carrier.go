package fixtures

import (
	"database/sql/driver"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func GetCarrier(id uint) *models.Carrier {
	return &models.Carrier{
		ID:               id,
		Name:             "UPS",
		TrackingTemplate: "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number",
		Scope:            "1",
	}
}

func ToCarrierPayload(carrier *models.Carrier) *payloads.Carrier {
	payload := &payloads.Carrier{
		Name:             carrier.Name,
		TrackingTemplate: carrier.TrackingTemplate,
	}
	payload.Scope = carrier.Scope

	return payload
}

func GetCarrierColumns() []string {
	return []string{"id", "name", "tracking_template"}
}

func GetCarrierRow(carrier *models.Carrier) []driver.Value {
	return []driver.Value{carrier.ID, carrier.Name, carrier.TrackingTemplate}
}
