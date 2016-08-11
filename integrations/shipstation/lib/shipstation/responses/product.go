package responses

// Product is a representation of the response we expect to get from ShipStation
// when querying their API for products.
type Product struct {
	ID                      int    `json:"productId"`
	SKU                     string `json:"sku"`
	Name                    string
	Price                   float64
	DefaultCost             float64
	Length                  float64
	Width                   float64
	Height                  float64
	WeightOz                float64
	InternalNotes           string
	FulfillmentSKU          string `json:"fulfillmentSku"`
	CreateDate              string
	ModifyDate              string
	Active                  bool
	ProductCategory         *ProductCategory
	ProductType             string
	WarehouseLocation       string
	DefaultCarrierCode      string
	DefaultServiceCode      string
	DefaultPackageCode      string
	DefaultIntlCarrierCode  string
	DefaultIntlServiceCode  string
	DefaultIntlPackageCode  string
	DefaultConfirmation     string
	DefaultIntlConfirmation string
	CustomsDescription      string
	CustomsValue            float64
	CustomsTariffNo         string
	CustomsCountryCode      string
	NoCustoms               bool
	Tags                    []ProductTag
}
