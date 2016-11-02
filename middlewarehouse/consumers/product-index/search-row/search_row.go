package searchrow

import (
	"errors"
	"log"

	"github.com/FoxComm/highlander/shared/golang/api"
)

type SearchRow struct {
	ProductID   int             `json:"productId"`
	Context     string          `json:"context"`
	SKUs        []string        `json:"skus"`
	Variants    []SearchVariant `json:"variants"`
	Title       string          `json:"title"`
	Description string          `json:"description"`
	Image       string          `json:"image"`
	SalePrice   int             `json:"salePrice"`
	Currency    string          `json:"currency"`
	Tags        interface{}     `json:"tags"`
}

func NewSearchRow(p *api.Product, pp PartialProduct) (*SearchRow, error) {
	if len(pp.AvailableSKUs) == 0 {
		return nil, errors.New("SearchRow must have at least one SKU")
	}

	row := new(SearchRow)
	row.ProductID = p.ID
	row.Context = p.Context.Name
	row.SKUs = pp.AvailableSKUs
	row.Description = p.Description()
	row.Tags = p.Tags()
	row.Variants = pp.Variants

	// Use the first SKU for any SKU-specific values
	code := pp.AvailableSKUs[0]
	var sku api.SKU
	for _, s := range p.SKUs {
		skuCode, err := s.Code()
		if err != nil {
			return nil, errors.New("Encountered a SKU with no code")
		}

		if code == skuCode {
			sku = s
			break
		}
	}

	title, err := sku.Title()
	if err != nil {
		return nil, err
	}

	if title == "" {
		title, err = p.Title()
		if err != nil {
			return nil, err
		}
	}
	row.Title = title

	image := sku.FirstImage()
	if image == "" {
		image = p.FirstImage()
	}
	row.Image = image

	price, err := sku.SalePrice()
	if err != nil {
		return nil, err
	}

	row.SalePrice = price.Value
	row.Currency = price.Currency

	log.Printf("Creating search row")
	log.Printf("ProductID: %d", row.ProductID)
	log.Printf("Context: %s", row.Context)
	log.Printf("SKUs: %q", row.SKUs)
	log.Printf("Search Variants: %q", row.Variants)
	log.Printf("Title: %s", row.Title)
	log.Printf("Description: %s", row.Description)
	log.Printf("Image: %s", row.Image)
	log.Printf("SalePrice: %d", row.SalePrice)
	log.Printf("Currency: %s", row.Currency)
	log.Printf("Tags: %q", row.Tags)

	return row, nil
}
