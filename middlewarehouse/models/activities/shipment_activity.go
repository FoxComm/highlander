package activities

import (
	"encoding/json"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func newShipmentActivity(aType string, shipment *models.Shipment, createdAt time.Time) (ISiteActivity, exceptions.IException) {
	resp, exception := responses.NewShipmentFromModel(shipment)
	if exception != nil {
		return nil, exception
	}

	shipBytes, err := json.Marshal(resp)
	if err != nil {
		return nil, NewActivityException(err)
	}

	return &defaultSiteActivity{
		ActivityId:   0, //we need to think of a way to create these. in phoenix the db takes care of it...
		ActivityType: aType,
		ActivityData: string(shipBytes),
		createdAt:    createdAt,
	}, nil
}
