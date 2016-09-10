package activities

import (
	"encoding/json"
	"time"

	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/models"
)

func newShipmentActivity(aType string, shipment *models.Shipment, createdAt time.Time) (ISiteActivity, error) {
	resp := responses.NewShipmentFromModel(shipment)
	shipBytes, err := json.Marshal(resp)
	if err != nil {
		return nil, err
	}

	return &defaultSiteActivity{
		activityType: aType,
		data:         string(shipBytes),
		createdAt:    createdAt,
	}, nil
}
