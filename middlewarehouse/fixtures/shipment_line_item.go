package fixtures

import (
	"database/sql/driver"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func GetShipmentLineItem(id uint, shipmentID uint, stockItemUnitId uint) *models.ShipmentLineItem {
	return &models.ShipmentLineItem{
		Base: gormfox.Base{
			ID: id,
		},
		ShipmentID:      shipmentID,
		ReferenceNumber: "e1a56545-8ada-4132-8d8c-b8aceda68bbe",
		StockItemUnitID: stockItemUnitId,
		SKU:             "SKU-TEST1",
		Name:            "Some shit",
		Price:           uint(3999),
		ImagePath:       "https://test.com/some-shit.png",
		Scope:           "1",
	}
}

func ToShipmentLineItemPayload(shipmentLineItem *models.ShipmentLineItem) *payloads.ShipmentLineItem {
	payload := &payloads.ShipmentLineItem{
		ID:              shipmentLineItem.ID,
		ReferenceNumber: shipmentLineItem.ReferenceNumber,
		SKU:             shipmentLineItem.SKU,
		Name:            shipmentLineItem.Name,
		Price:           shipmentLineItem.Price,
		ImagePath:       shipmentLineItem.ImagePath,
	}

	payload.Scope = shipmentLineItem.Scope

	return payload
}

func GetShipmentLineItemColumns() []string {
	return []string{"id", "shipment_id", "stock_item_unit_id", "name", "reference_number",
		"sku", "price", "image_path", "created_at", "updated_at", "deleted_at"}
}

func GetShipmentLineItemRow(shipmentLineItem *models.ShipmentLineItem) []driver.Value {
	return []driver.Value{shipmentLineItem.ID, shipmentLineItem.ShipmentID, shipmentLineItem.StockItemUnitID,
		shipmentLineItem.Name, shipmentLineItem.ReferenceNumber, shipmentLineItem.SKU, shipmentLineItem.Price,
		shipmentLineItem.ImagePath, shipmentLineItem.CreatedAt, shipmentLineItem.UpdatedAt, shipmentLineItem.DeletedAt}
}
