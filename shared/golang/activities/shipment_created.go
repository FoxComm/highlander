package activities

import (
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func NewShipmentCreated(shipment *models.Shipment, createdAt time.Time) (SiteActivity, error) {
	activityType := "shipment_created"
	return newShipmentActivity(activityType, shipment, createdAt)
}
