package activities

import (
	"encoding/json"
	"time"

	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/models"
)

func NewShipmentCreated(shipment *models.Shipment, createdAt time.Time) (SiteActivity, error) {
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
