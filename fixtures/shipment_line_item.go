package fixtures

import (
	"database/sql/driver"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
	"github.com/FoxComm/middlewarehouse/models"
)

func GetShipmentLineItem(id uint, shipmentID uint, stockItemUnitId uint) *models.ShipmentLineItem {
	return &models.ShipmentLineItem{
		Base: gormfox.Base{
			ID: id,
		},
		ShipmentID:      shipmentID,
		StockItemUnitID: stockItemUnitId,
		SKU:             "SKU-TEST1",
		Name:            "Some shit",
		Price:           uint(3999),
		ImagePath:       "https://test.com/some-shit.png",
	}
}

func ToShipmentLineItemPayload(shipmentLineItem *models.ShipmentLineItem) *payloads.ShipmentLineItem {
	return &payloads.ShipmentLineItem{
		ID:        shipmentLineItem.ID,
		SKU:       shipmentLineItem.SKU,
		Name:      shipmentLineItem.Name,
		Price:     shipmentLineItem.Price,
		ImagePath: shipmentLineItem.ImagePath,
	}
}

func GetShipmentLineItemColumns() []string {
	return []string{"id", "shipment_id", "stock_item_unit_id", "name", "sku", "price",
		"image_path", "created_at", "updated_at", "deleted_at"}
}

func GetShipmentLineItemRow(shipmentLineItem *models.ShipmentLineItem) []driver.Value {
	return []driver.Value{shipmentLineItem.ID, shipmentLineItem.ShipmentID, shipmentLineItem.StockItemUnitID,
		shipmentLineItem.Name, shipmentLineItem.SKU, shipmentLineItem.Price, shipmentLineItem.ImagePath,
		shipmentLineItem.CreatedAt, shipmentLineItem.UpdatedAt, shipmentLineItem.DeletedAt}
}
