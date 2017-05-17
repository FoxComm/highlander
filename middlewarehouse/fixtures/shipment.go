package fixtures

import (
	"database/sql"
	"database/sql/driver"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func GetShipment(
	id uint,
	orderRefNum string,
	shippingMethodCode string,
	shippingMethod *models.ShippingMethod,
	addressID uint,
	address *models.Address,
	lineItems []models.ShipmentLineItem,
) *models.Shipment {
	return &models.Shipment{
		Base: gormfox.Base{
			ID: id,
		},
		ReferenceNumber:    "BR10005",
		ShippingMethodCode: shippingMethodCode,
		ShippingMethod:     *shippingMethod,
		OrderRefNum:        orderRefNum,
		State:              models.ShipmentStatePending,
		ShipmentDate:       sql.NullString{},
		EstimatedArrival:   sql.NullString{},
		DeliveredDate:      sql.NullString{},
		AddressID:          addressID,
		Address:            *address,
		ShipmentLineItems:  lineItems,
		TrackingNumber:     sql.NullString{},
		Scope:              "1",
	}
}

func GetShipmentShort(id uint) *models.Shipment {
	shippingMethod1 := GetShippingMethod(uint(1), uint(1), GetCarrier(uint(1)))
	address1 := GetAddress(uint(0), uint(1), GetRegion(uint(1), uint(1), GetCountry(uint(1))))
	shipmentLineItem1 := GetShipmentLineItem(uint(1), id, uint(1))
	shipmentLineItem2 := GetShipmentLineItem(uint(2), id, uint(2))

	return GetShipment(id, "BR10005", shippingMethod1.Code, shippingMethod1, address1.ID, address1,
		[]models.ShipmentLineItem{*shipmentLineItem1, *shipmentLineItem2})
}

func ToShipmentPayload(model *models.Shipment) *payloads.CreateShipment {
	shipment := &payloads.CreateShipment{
		ShippingMethodCode: model.ShippingMethodCode,
		OrderRefNum:        model.OrderRefNum,
		State:              string(model.State),
		ShipmentDate:       responses.NewStringFromSqlNullString(model.ShipmentDate),
		EstimatedArrival:   responses.NewStringFromSqlNullString(model.EstimatedArrival),
		DeliveredDate:      responses.NewStringFromSqlNullString(model.DeliveredDate),
		Address:            *ToAddressPayload(&model.Address),
		TrackingNumber:     responses.NewStringFromSqlNullString(model.TrackingNumber),
	}

	for _, lineItem := range model.ShipmentLineItems {
		shipment.ShipmentLineItems = append(shipment.ShipmentLineItems, *ToShipmentLineItemPayload(&lineItem))
	}

	shipment.Scope = model.Scope

	return shipment
}

func GetShipmentColumns() []string {
	return []string{"id", "shipping_method_code", "address_id", "order_ref_num", "state", "shipment_date",
		"estimated_arrival", "delivered_date", "tracking_number", "created_at", "updated_at", "deleted_at"}
}

func GetShipmentRow(shipment *models.Shipment) []driver.Value {
	return []driver.Value{shipment.ID, shipment.ShippingMethodCode, shipment.AddressID, shipment.OrderRefNum,
		[]uint8(shipment.State), nil, nil, nil, nil, shipment.CreatedAt, shipment.UpdatedAt, shipment.DeletedAt}
}
