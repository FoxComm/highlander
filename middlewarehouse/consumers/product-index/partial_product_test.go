package main

import (
	"encoding/json"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/consumers/product-index/utils"
	"github.com/FoxComm/highlander/shared/golang/api"
)

func TestSingleVisualVariant(t *testing.T) {
	productByte := []byte(utils.ProductWithOneVisualVariant)
	product := new(api.Product)

	if err := json.Unmarshal(productByte, product); err != nil {
		t.Errorf("Error unmarshalling product with error: %s", err.Error())
		return
	}

	visualVariants := []string{"Color"}
	partialProducts, err := MakePartialProducts(product, visualVariants)
	if err != nil {
		t.Errorf("Error creating partial products with error: %s", err.Error())
		return
	}

	if len(partialProducts) != 2 {
		t.Errorf("Expected 2 partial products, got %d", len(partialProducts))
		return
	}

	for _, prod := range partialProducts {
		row, err := NewSearchRow(*product, prod)
		if err != nil {
			t.Errorf("Error creating search row with error: %s", err.Error())
			return
		}

		if len(row.SKUs) != 2 {
			t.Errorf("Expected 2 SKUs, got %d", len(row.SKUs))
			return
		}

		if row.Title != "Nike Free Flyknit" {
			t.Errorf("Expected title to be Nike Free Flyknit, got %s", row.Title)
			return
		}
	}
}

func TestMultipleVisualVariants(t *testing.T) {
	productByte := []byte(utils.ProductMultipleVisualVariants)
	product := new(api.Product)

	if err := json.Unmarshal(productByte, product); err != nil {
		t.Errorf("Error unmarshalling product with error: %s", err.Error())
		return
	}

	visualVariants := []string{"Color", "Fabric"}
	partialProducts, err := MakePartialProducts(product, visualVariants)
	if err != nil {
		t.Errorf("Error creating partial products with error: %s", err.Error())
		return
	}

	if len(partialProducts) != 4 {
		t.Errorf("Expected 4 partial products, got %d", len(partialProducts))
		return
	}

	for _, prod := range partialProducts {
		row, err := NewSearchRow(*product, prod)
		if err != nil {
			t.Errorf("Error creating search row with error: %s", err.Error())
			return
		}

		if row.Title != "Nike Free" {
			t.Errorf("Expected title to be Nike Free, got %s", row.Title)
			return
		}
	}
}

func TestNoVariants(t *testing.T) {
	productByte := []byte(utils.ProductNoVariants)
	product := new(api.Product)

	if err := json.Unmarshal(productByte, product); err != nil {
		t.Errorf("Error unmarshalling product with error: %s", err.Error())
		return
	}

	visualVariants := []string{"Color", "Fabric"}
	partialProducts, err := MakePartialProducts(product, visualVariants)
	if err != nil {
		t.Errorf("Error creating partial products with error: %s", err.Error())
		return
	}

	if len(partialProducts) != 1 {
		t.Errorf("Expected 1 partial products, got %d", len(partialProducts))
		return
	}

	for _, prod := range partialProducts {
		row, err := NewSearchRow(*product, prod)
		if err != nil {
			t.Errorf("Error creating search row with error: %s", err.Error())
			return
		}

		if row.Title != "Duckling" {
			t.Errorf("Expected title to be Duckling, got %s", row.Title)
			return
		}
	}
}
