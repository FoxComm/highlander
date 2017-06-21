package payloads

// Product is the payload taking by the update product endpoint.
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
	FulfillmentSKU          *string `json:"fulfillmentSku"`
	Active                  bool
	ProductCategory         *int
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
