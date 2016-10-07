package activities

import (
	"encoding/json"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func newShipmentActivity(aType string, shipment *models.Shipment, createdAt time.Time) (ISiteActivity, error) {
	resp := responses.NewShipmentFromModel(shipment)
	shipBytes, err := json.Marshal(resp)
	if err != nil {
		return nil, err
	}

	return &defaultSiteActivity{
		ActivityType: aType,
		ActivityData: string(shipBytes),
		createdAt:    createdAt,
	}, nil
}
