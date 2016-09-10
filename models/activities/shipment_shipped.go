package activities

import (
	"time"

	"github.com/FoxComm/middlewarehouse/models"
)

func NewShipmentShipped(shipment *models.Shipment, updatedAt time.Time) (ISiteActivity, error) {
	activityType := "shipment_shipped"
	return newShipmentActivity(activityType, shipment, updatedAt)
}
