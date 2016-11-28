package activities

import (
	"encoding/json"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func newShipmentActivity(aType string, shipment *models.Shipment, createdAt time.Time) (ISiteActivity, error) {
	resp, err := responses.NewShipmentFromModel(shipment)
	if err != nil {
		return nil, err
	}

	shipBytes, err := json.Marshal(resp)
	if err != nil {
		return nil, err
	}

	return &defaultSiteActivity{
		ActivityId:   0, //we need to think of a way to create these. in phoenix the db takes care of it...
		ActivityType: aType,
		ActivityData: string(shipBytes),
		createdAt:    createdAt,
	}, nil
}
