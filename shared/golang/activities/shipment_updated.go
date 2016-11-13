package activities

import (
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func NewShipmentUpdated(shipment *models.Shipment, updatedAt time.Time) (SiteActivity, error) {
	activityType := "shipment_updated"
	return newShipmentActivity(activityType, shipment, updatedAt)
}
