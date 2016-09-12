package activities

import (
	"time"

	"github.com/FoxComm/middlewarehouse/models"
)

func NewShipmentCreated(shipment *models.Shipment, createdAt time.Time) (ISiteActivity, error) {
	activityType := "shipment_created"
	return newShipmentActivity(activityType, shipment, createdAt)
}
