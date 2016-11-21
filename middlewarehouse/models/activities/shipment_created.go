package activities

import (
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func NewShipmentCreated(shipment *models.Shipment, createdAt time.Time) (ISiteActivity, exceptions.IException) {
	activityType := "shipment_created"
	return newShipmentActivity(activityType, shipment, createdAt)
}
