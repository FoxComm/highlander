package fixtures

import (
	"database/sql"
	"database/sql/driver"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
	"github.com/FoxComm/middlewarehouse/models"
)

func GetShipment(
	id uint,
	shippingMethodID uint,
	shippingMethod *models.ShippingMethod,
	addressID uint,
	address *models.Address,
	lineItems []models.ShipmentLineItem,
) *models.Shipment {
	return &models.Shipment{
		Base: gormfox.Base{
			ID: id,
		},
		ShippingMethodID:  shippingMethodID,
		ShippingMethod:    *shippingMethod,
		ReferenceNumber:   "BR1005",
		State:             models.ShipmentStatePending,
		ShipmentDate:      sql.NullString{},
		EstimatedArrival:  sql.NullString{},
		DeliveredDate:     sql.NullString{},
		AddressID:         addressID,
		Address:           *address,
		ShipmentLineItems: lineItems,
		TrackingNumber:    sql.NullString{},
	}
}

func GetShipmentShort(id uint) *models.Shipment {
	shippingMethod1 := GetShippingMethod(uint(1), uint(1), GetCarrier(uint(1)))
	address1 := GetAddress(uint(1), uint(1), GetRegion(uint(1), uint(1), GetCountry(uint(1))))
	shipmentLineItem1 := GetShipmentLineItem(uint(1), id)
	shipmentLineItem2 := GetShipmentLineItem(uint(2), id)

	return GetShipment(id, shippingMethod1.ID, shippingMethod1, address1.ID, address1,
		[]models.ShipmentLineItem{*shipmentLineItem1, *shipmentLineItem2})
}

func ToShipmentPayload(model *models.Shipment) *payloads.Shipment {
	shipment := &payloads.Shipment{
		ShippingMethodID: model.ShippingMethodID,
		ReferenceNumber:  model.ReferenceNumber,
		State:            string(model.State),
		ShipmentDate:     responses.NewStringFromSqlNullString(model.ShipmentDate),
		EstimatedArrival: responses.NewStringFromSqlNullString(model.EstimatedArrival),
		DeliveredDate:    responses.NewStringFromSqlNullString(model.DeliveredDate),
		Address:          *ToAddressPayload(&model.Address),
		TrackingNumber:   responses.NewStringFromSqlNullString(model.TrackingNumber),
	}

	for _, lineItem := range model.ShipmentLineItems {
		shipment.ShipmentLineItems = append(shipment.ShipmentLineItems, *ToShipmentLineItemPayload(&lineItem))
	}

	return shipment
}

func GetShipmentColumns() []string {
	return []string{"id", "shipping_method_id", "address_id", "reference_number", "state", "shipment_date",
		"estimated_arrival", "delivered_date", "tracking_number", "created_at", "updated_at", "deleted_at"}
}

func GetShipmentRow(shipment *models.Shipment) []driver.Value {
	return []driver.Value{shipment.ID, shipment.ShippingMethodID, shipment.AddressID, shipment.ReferenceNumber,
		[]uint8(shipment.State), nil, nil, nil, nil, shipment.CreatedAt, shipment.UpdatedAt, shipment.DeletedAt}
}
