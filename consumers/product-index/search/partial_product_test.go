package search

import (
	"testing"

	"github.com/FoxComm/highlander/consumers/product-index/utils"
)

func TestSingleVisualVariant(t *testing.T) {
	product := utils.NewProductWithOneVisualVariant()

	visualVariants := []string{"color"}
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
		row, err := NewSearchRow(product, prod)
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
	product := utils.NewProductWithMultipleVisualVariants()
	visualVariants := []string{"color", "fabric"}
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
		row, err := NewSearchRow(product, prod)
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
	product := utils.NewProductWithNoVariants()
	visualVariants := []string{"color", "fabric"}
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
		row, err := NewSearchRow(product, prod)
		if err != nil {
			t.Errorf("Error creating search row with error: %s", err.Error())
			return
		}

		if row.Title != "Nike Free Flyknit" {
			t.Errorf("Expected title to be Nike Free Flyknit, got %s", row.Title)
			return
		}
	}
}
