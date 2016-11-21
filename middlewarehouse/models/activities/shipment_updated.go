package activities

import (
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

func NewShipmentUpdated(shipment *models.Shipment, updatedAt time.Time) (ISiteActivity, exceptions.IException) {
	activityType := "shipment_updated"
	return newShipmentActivity(activityType, shipment, updatedAt)
}
