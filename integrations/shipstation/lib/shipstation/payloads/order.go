package payloads

// Order respresents an order in ShipStation.
type Order struct {
	OrderNumber              string
	OrderKey                 *string
	OrderDate                string
	PaymentDate              *string
	ShipByDate               *string
	OrderStatus              string
	CustomerUsername         *string
	CustomerEmail            *string
	BillTo                   Address
	ShipTo                   Address
	Items                    []OrderItem
	AmountPaid               float64
	TaxAmount                float64
	ShippingAmount           float64
	CustomerNotes            string
	InternalNote             string
	Gift                     bool
	GiftMessage              string
	PaymentMethod            string
	RequestedShippingService string
	CarrierCode              string
	ServiceCode              string
	PackageCode              string
	Confirmation             string
	ShipDate                 string
	HoldUntilDate            string
	Weight                   Weight
	Dimensions               Dimensions
	InsuranceOptions         InsuranceOptions
	AdvancedOptions          AdvancedOptions
	TagIDs                   []int `json:"tagIds"`
}
