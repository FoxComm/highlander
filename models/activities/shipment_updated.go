package activities

import (
	"time"

	"github.com/FoxComm/middlewarehouse/models"
)

func NewShipmentUpdated(shipment *models.Shipment, updatedAt time.Time) (ISiteActivity, error) {
	activityType := "shipment_updated"
	return newShipmentActivity(activityType, shipment, updatedAt)
}
