package models

const (
	ShippingTypeFlat = iota
	ShippingTypeVariable
)

type ShippingMethod struct {
	ID           uint
	CarrierID    uint
	Carrier      Carrier
	Name         string
	Code         string
	ShippingType int
	Cost         uint
	Scope        string
}

func (shippingMethod *ShippingMethod) Identifier() uint {
	return shippingMethod.ID
}
