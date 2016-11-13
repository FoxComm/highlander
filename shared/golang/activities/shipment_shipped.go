package activities

import (
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func NewShipmentShipped(shipment *models.Shipment, updatedAt time.Time) (SiteActivity, error) {
	activityType := "shipment_shipped"
	return newShipmentActivity(activityType, shipment, updatedAt)
}
