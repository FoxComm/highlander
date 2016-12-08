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
		ShipmentID:       shipmentID,
		ReferenceNumbers: []string{"e1a56545-8ada-4132-8d8c-b8aceda68bbe"},
		StockItemUnitID:  stockItemUnitId,
		SkuID:            1,
		SkuCode:          "SKU-TEST1",
		Name:             "Some shit",
		Price:            uint(3999),
		ImagePath:        "https://test.com/some-shit.png",
	}
}

func ToShipmentLineItemPayload(shipmentLineItem *models.ShipmentLineItem) *payloads.ShipmentLineItem {
	return &payloads.ShipmentLineItem{
		ID:               shipmentLineItem.ID,
		ReferenceNumbers: shipmentLineItem.ReferenceNumbers,
		SkuID:            shipmentLineItem.SkuID,
		SkuCode:          shipmentLineItem.SkuCode,
		Name:             shipmentLineItem.Name,
		Price:            shipmentLineItem.Price,
		ImagePath:        shipmentLineItem.ImagePath,
	}
}

func GetShipmentLineItemColumns() []string {
	return []string{"id", "shipment_id", "stock_item_unit_id", "name", "reference_number",
		"sku_id", "sku_code", "price", "image_path", "created_at", "updated_at", "deleted_at"}
}

func GetShipmentLineItemRow(shipmentLineItem *models.ShipmentLineItem) []driver.Value {
	return []driver.Value{shipmentLineItem.ID, shipmentLineItem.ShipmentID, shipmentLineItem.StockItemUnitID,
		shipmentLineItem.Name, shipmentLineItem.ReferenceNumbers, shipmentLineItem.SkuID, shipmentLineItem.SkuCode,
		shipmentLineItem.Price, shipmentLineItem.ImagePath, shipmentLineItem.CreatedAt, shipmentLineItem.UpdatedAt,
		shipmentLineItem.DeletedAt}
}
