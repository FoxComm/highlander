package payloads

import "github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/api/responses"

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

// FromResponse initializes the payload based on a previous response.
func (p *Product) FromResponse(resp *responses.Product) {
	p.ID = resp.ID
	p.SKU = resp.SKU
	p.Name = resp.Name
	p.Price = resp.Price
	p.DefaultCost = resp.DefaultCost
	p.Length = resp.Length
	p.Width = resp.Width
	p.Height = resp.Height
	p.WeightOz = resp.WeightOz
	p.InternalNotes = resp.InternalNotes
	p.Active = resp.Active
	p.ProductType = resp.ProductType
	p.WarehouseLocation = resp.WarehouseLocation
	p.DefaultCarrierCode = resp.DefaultCarrierCode
	p.DefaultServiceCode = resp.DefaultServiceCode
	p.DefaultPackageCode = resp.DefaultPackageCode
	p.DefaultIntlCarrierCode = resp.DefaultIntlCarrierCode
	p.DefaultIntlServiceCode = resp.DefaultIntlServiceCode
	p.DefaultIntlPackageCode = resp.DefaultIntlPackageCode
	p.DefaultConfirmation = resp.DefaultConfirmation
	p.DefaultIntlConfirmation = resp.DefaultIntlConfirmation
	p.CustomsDescription = resp.CustomsDescription
	p.CustomsValue = resp.CustomsValue
	p.CustomsTariffNo = resp.CustomsTariffNo
	p.CustomsCountryCode = resp.CustomsCountryCode
	p.NoCustoms = resp.NoCustoms

	if resp.FulfillmentSKU != "" {
		p.FulfillmentSKU = &(resp.FulfillmentSKU)
	}

	if resp.ProductCategory != nil {
		productCategory := resp.ProductCategory.ID
		p.ProductCategory = &productCategory
	}
}
