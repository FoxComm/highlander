package activities

import (
	"encoding/json"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func NewShipmentCreated(shipment *models.Shipment, createdAt time.Time) (ISiteActivity, error) {
	resp := responses.NewShipmentFromModel(shipment)

	activityType := "shipment_created"

	shipBytes, err := json.Marshal(resp)
	if err != nil {
		return nil, err
	}

	return &defaultSiteActivity{
		activityType: activityType,
		data:         string(shipBytes),
		createdAt:    createdAt,
	}, nil
}
