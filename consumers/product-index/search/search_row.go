package search

import (
	"errors"
	"fmt"
	"log"

	"encoding/json"
	"github.com/FoxComm/highlander/shared/golang/api"
)

type SearchRow struct {
	ProductID   int             `json:"productId"`
	Context     string          `json:"context"`
	SKUs        []SearchSKU     `json:"skus"`
	Variants    []SearchVariant `json:"variants"`
	Title       string          `json:"title"`
	Description string          `json:"description"`
	Image       string          `json:"image"`
	SalePrice   int             `json:"salePrice"`
	Currency    string          `json:"currency"`
	Tags        interface{}     `json:"tags"`
}

func (s SearchRow) Identifier() string {
	if len(s.SKUs) > 0 {
		return fmt.Sprintf("%d-%d", s.ProductID, s.SKUs[0].ID)
	}

	return fmt.Sprintf("%d", s.ProductID)
}

type SearchSKU struct {
	ID   int    `json:"skuId"`
	Code string `json:"code"`
}

func createSearchMap(p *api.Product) (map[string]int, error) {
	sm := map[string]int{}
	for _, sku := range p.SKUs {
		code, err := sku.Code()
		if err != nil {
			return sm, err
		}

		sm[code] = sku.ID
	}

	return sm, nil
}

func NewSearchRow(p *api.Product, pp PartialProduct) (*SearchRow, error) {
	if len(pp.AvailableSKUs) == 0 {
		return nil, errors.New("SearchRow must have at least one SKU")
	}

	row := new(SearchRow)
	row.ProductID = p.ID
	row.Context = p.Context.Name
	row.Description = p.Description()
	row.Tags = p.Tags()
	row.Variants = pp.Variants

	ss := []SearchSKU{}
	sm, err := createSearchMap(p)
	if err != nil {
		return nil, err
	}

	for _, code := range pp.AvailableSKUs {
		id, ok := sm[code]
		if !ok {
			return nil, fmt.Errorf("Unable to find ID for SKU %s", code)
		}

		searchSku := SearchSKU{ID: id, Code: code}
		ss = append(ss, searchSku)
	}

	row.SKUs = ss

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

	row.Title, err = p.Title()
	if err != nil {
		return nil, err
	}

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

func EnrichRowWithAttributes(p *api.Product, row *SearchRow, searchCfg *Config) (map[string]interface{}, error) {
	var productMap map[string]interface{}
	productMap = make(map[string]interface{})

	body, err := json.Marshal(row)
	if err != nil {
		return nil, err
	}

	err = json.Unmarshal(body, &productMap)
	if err != nil {
		return nil, err
	}

	for _, attr := range searchCfg.Attributes {
		attrVal, err := p.Attributes.LookupValue(attr.Name)
		if err != nil {
			return nil, err
		}
		productMap[attr.Name] = attrVal
	}

	return productMap, nil
}
