package fixtures

import (
	"database/sql/driver"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
	"github.com/FoxComm/middlewarehouse/models"
)

func GetShipmentLineItem(id uint, shipmentID uint) *models.ShipmentLineItem {
	return &models.ShipmentLineItem{
		Base: gormfox.Base{
			ID: id,
		},
		ShipmentID:      shipmentID,
		ReferenceNumber: "BR1002",
		SKU:             "SKU-TEST1",
		Name:            "Some shit",
		Price:           uint(3999),
		ImagePath:       "https://test.com/some-shit.png",
		State:           models.ShipmentStatePending,
	}
}

func ToShipmentLineItemPayload(shipmentLineItem *models.ShipmentLineItem) *payloads.ShipmentLineItem {
	return &payloads.ShipmentLineItem{
		ID:              shipmentLineItem.ID,
		ReferenceNumber: shipmentLineItem.ReferenceNumber,
		SKU:             shipmentLineItem.SKU,
		Name:            shipmentLineItem.Name,
		Price:           shipmentLineItem.Price,
		ImagePath:       shipmentLineItem.ImagePath,
		State:           string(shipmentLineItem.State),
	}
}

func GetShipmentLineItemColumns() []string {
	return []string{"id", "shipment_id", "name", "reference_number", "sku", "price",
		"image_path", "state", "created_at", "updated_at", "deleted_at"}
}

func GetShipmentLineItemRow(shipmentLineItem *models.ShipmentLineItem) []driver.Value {
	return []driver.Value{shipmentLineItem.ID, shipmentLineItem.ShipmentID, shipmentLineItem.Name,
		shipmentLineItem.ReferenceNumber, shipmentLineItem.SKU, shipmentLineItem.Price, shipmentLineItem.ImagePath,
		[]uint8(shipmentLineItem.State), shipmentLineItem.CreatedAt, shipmentLineItem.UpdatedAt, shipmentLineItem.DeletedAt}
}
