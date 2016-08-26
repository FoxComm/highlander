package activities

import (
	"encoding/json"
	"time"

	"github.com/FoxComm/middlewarehouse/api/responses"
)

const activityType = "shipment_created"

func NewShipmentCreated(shipment *responses.Shipment, createdAt time.Time) (SiteActivity, error) {
	shipBytes, err := json.Marshal(shipment)
	if err != nil {
		return nil, err
	}

	return &defaultSiteActivity{
		activityType: activityType,
		data:         string(shipBytes),
		createdAt:    createdAt,
	}, nil
}
