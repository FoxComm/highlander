package search

import (
	"strings"

	"github.com/FoxComm/highlander/consumers/product-index/utils"
	"github.com/FoxComm/highlander/shared/golang/api"
)

type PartialProduct struct {
	AvailableSKUs []string
	Variants      []SearchVariant
}

func MakePartialProducts(product *api.Product, visualVariants []string) ([]PartialProduct, error) {
	baseProduct := PartialProduct{}
	for _, sku := range product.SKUs {
		code, err := sku.Code()
		if err != nil {
			return nil, err
		}

		baseProduct.AvailableSKUs = append(baseProduct.AvailableSKUs, code)
	}

	return makeProducts(product.Variants, baseProduct, visualVariants)
}

// makeProducts is a recursive function that iterates through a list of variants
// and returns a slice of PartialProduct. If no visual variants are defined, the
// PartialProduct is the product.
func makeProducts(variants []api.Variant, current PartialProduct, visualVariants []string) ([]PartialProduct, error) {
	// If there are no variants to process, we're in the terminal state.
	if len(variants) == 0 {
		return []PartialProduct{current}, nil
	}

	// Pop-off the first variant and analyze it if it's a visual variant.
	head := variants[0]
	tail := variants[1:]

	variantName, err := head.Name()
	if err != nil {
		return nil, err
	}

	if !isVisual(variantName, visualVariants) {
		return makeProducts(tail, current, visualVariants)
	}

	// Process each VariantValue on the head and create PartialProducts with the
	// Value's SKUs that intersect with the current PartialProduct.
	partialProducts := []PartialProduct{}
	for _, value := range variants[0].Values {
		variant := SearchVariant{
			VariantID:        variants[0].ID,
			VariantName:      variantName,
			VariantValueID:   value.ID,
			VariantValueName: value.Name,
		}

		partialProduct := PartialProduct{
			AvailableSKUs: utils.GetIntersection(current.AvailableSKUs, value.SKUCodes),
			Variants:      append(current.Variants, variant),
		}

		newProducts, err := makeProducts(tail, partialProduct, visualVariants)
		if err != nil {
			return nil, err
		}

		partialProducts = append(partialProducts, newProducts...)
	}

	return partialProducts, nil
}

func isVisual(name string, variants []string) bool {
	if len(variants) == 0 {
		return true
	}

	nameArr := []string{strings.ToLower(name)}
	return len(utils.GetIntersection(nameArr, variants)) != 0
}
