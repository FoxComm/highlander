package main

import (
	"errors"

	"github.com/FoxComm/highlander/shared/golang/api"
)

type PartialProduct struct {
	AvailableSKUs []string
	Variants      map[string]string
}

func (p PartialProduct) SearchRow() (SearchRow, error) {
	row := SearchRow{}
	if len(p.AvailableSKUs) == 0 {
		return row, errors.New("SearchRow must have at least one SKU")
	}

	row.SKUs = p.AvailableSKUs
	row.Variants = p.Variants

	return row, nil
}

func MakePartialProducts(variants []api.Variant, state PartialProduct, visualVariants []string) ([]PartialProduct, error) {
	mappings := []PartialProduct{}
	if len(variants) == 0 {
		return mappings, nil
	}

	tail := variants[1:]

	variantName, err := variants[0].Name()
	if err != nil {
		return mappings, err
	}

	if !isVisual(variantName, visualVariants) {
		return MakePartialProducts(tail, state, visualVariants)
	}

	for _, value := range variants[0].Values {
		var nas []string
		if len(state.AvailableSKUs) == 0 {
			nas = value.SKUCodes
		} else {
			nas = getIntersection(state.AvailableSKUs, value.SKUCodes)
		}

		newVariants := map[string]string{variantName: value.Name}
		for k, v := range state.Variants {
			newVariants[k] = v
		}

		mapping := PartialProduct{AvailableSKUs: nas, Variants: newVariants}

		if len(tail) == 0 {
			mappings = append(mappings, mapping)
		} else {
			newMappings, err := MakePartialProducts(tail, mapping, visualVariants)
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

	nameArr := []string{name}
	return len(getIntersection(nameArr, variants)) != 0
}
