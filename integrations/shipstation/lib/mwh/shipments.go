package mwh

//func NewShipmentFromOrder(o *phoenix.Order) (*payloads.Shipment, error) {
//if len(*o.ShippingAddresses) != 1 {
//return nil, errors.New("Order must have one shipping address")
//}

//if len(*o.LineItems) == 0 {
//return nil, errors.New("Order must have at least one line item")
//}

//sa := (*o.ShippingAddresses)[0]

//var address2 *string
//if sa.Address2 != "" {
//address2 = &(sa.Address2)
//}

//shippingAddress := payloads.Address{
//Region:   sa.Region,
//Country:  sa.Country,
//City:     sa.City,
//Address1: sa.Address1,
//Address2: address2,
//Zip:      sa.Zip,
//}

//lineItems := make([]payloads.ShipmentLineItem, len(*o.LineItems))
//for idx, oli := range *o.LineItems {
//lineItems[idx] = payloads.ShipmentLineItem{
//ReferenceNumber: oli.ReferenceNumber,
//SKU:             oli.SKU,
//Name:            oli.Name,
//State:           "pending",
//Price:           oli.Price,
//ImagePath:       oli.ImagePath,
//}
//}

//shipment := &payloads.Shipment{
//ShippingMethodID:  o.ShippingMethod.ShippingMethodID,
//ReferenceNumber:   o.ReferenceNumber,
//State:             "pending",
//Address:           shippingAddress,
//ShipmentLineItems: lineItems,
//}

//return shipment, nil
//}
