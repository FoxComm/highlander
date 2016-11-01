package searchrow

import (
	"strings"

	"github.com/FoxComm/highlander/middlewarehouse/consumers/product-index/utils"
	"github.com/FoxComm/highlander/shared/golang/api"
)

type PartialProduct struct {
	AvailableSKUs []string
	Variants      map[string]string
}

func MakePartialProducts(product *api.Product, visualVariants []string) ([]PartialProduct, error) {
	variants := product.Variants
	if len(variants) == 0 {
		code, err := product.SKUs[0].Code()
		if err != nil {
			return []PartialProduct{}, err
		}

		return []PartialProduct{
			PartialProduct{AvailableSKUs: []string{code}},
		}, nil
	}

	return makeProducts(variants, PartialProduct{}, visualVariants)
}

func makeProducts(variants []api.Variant, state PartialProduct, visualVariants []string) ([]PartialProduct, error) {
	mappings := []PartialProduct{}
	if len(variants) == 0 {
		return []PartialProduct{state}, nil
	}

	tail := variants[1:]

	variantName, err := variants[0].Name()
	if err != nil {
		return mappings, err
	}

	if !isVisual(variantName, visualVariants) {
		return makeProducts(tail, state, visualVariants)
	}

	for _, value := range variants[0].Values {
		var nas []string
		if len(state.AvailableSKUs) == 0 {
			nas = value.SKUCodes
		} else {
			nas = utils.GetIntersection(state.AvailableSKUs, value.SKUCodes)
		}

		newVariants := map[string]string{variantName: value.Name}
		for k, v := range state.Variants {
			newVariants[k] = v
		}

		mapping := PartialProduct{AvailableSKUs: nas, Variants: newVariants}

		if len(tail) == 0 {
			mappings = append(mappings, mapping)
		} else {
			newMappings, err := makeProducts(tail, mapping, visualVariants)
			if err != nil {
				return mappings, err
			}

			mappings = append(mappings, newMappings...)
		}
	}

	return mappings, nil
}

func isVisual(name string, variants []string) bool {
	if len(variants) == 0 {
		return true
	}

	nameArr := []string{strings.ToLower(name)}
	return len(utils.GetIntersection(nameArr, variants)) != 0
}
