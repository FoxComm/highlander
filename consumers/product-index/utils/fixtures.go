package utils

import "github.com/FoxComm/highlander/shared/golang/api"

func MakeContext() api.Context {
	return api.Context{
		Name: "default",
		Attributes: map[string]string{
			"lang":     "en",
			"modality": "desktop",
		},
	}
}

func NewProductWithOneVisualVariant() *api.Product {
	colorValues := []api.VariantValue{
		api.VariantValue{
			ID:     1,
			Name:   "White",
			Swatch: "FFFFFF",
			SKUCodes: []string{
				"FREE-FLYKNIT-WHITE-90",
				"FREE-FLYKNIT-WHITE-100",
			},
		},
		api.VariantValue{
			ID:     2,
			Name:   "Orange",
			Swatch: "FF4500",
			SKUCodes: []string{
				"FREE-FLYKNIT-ORANGE-90",
				"FREE-FLYKNIT-ORANGE-100",
			},
		},
	}

	sizeValues := []api.VariantValue{
		api.VariantValue{
			ID:   3,
			Name: "9.0",
			SKUCodes: []string{
				"FREE-FLYKNIT-WHITE-90",
				"FREE-FLYKNIT-ORANGE-90",
			},
		},
		api.VariantValue{
			ID:   4,
			Name: "10.0",
			SKUCodes: []string{
				"FREE-FLYKNIT-WHITE-100",
				"FREE-FLYKNIT-ORANGE-100",
			},
		},
	}

	colorVariant := makeVariant(12, "Color", "color", colorValues)
	sizeVariant := makeVariant(13, "Size", "size", sizeValues)

	return &api.Product{
		ID:         1,
		Context:    MakeContext(),
		Attributes: makeProductAttributes("Nike Free Flyknit"),
		SKUs: []api.SKU{
			SKUs["FREE-FLYKNIT-ORANGE-90"],
			SKUs["FREE-FLYKNIT-ORANGE-100"],
			SKUs["FREE-FLYKNIT-WHITE-90"],
			SKUs["FREE-FLYKNIT-WHITE-100"],
		},
		Variants: []api.Variant{
			colorVariant,
			sizeVariant,
		},
	}
}

func NewProductWithMultipleVisualVariants() *api.Product {
	colorValues := []api.VariantValue{
		api.VariantValue{
			ID:     1,
			Name:   "White",
			Swatch: "FFFFFF",
			SKUCodes: []string{
				"FREE-FLYKNIT-WHITE-90",
				"FREE-FLYKNIT-WHITE-100",
				"FREE-FABRIC-WHITE-90",
				"FREE-FABRIC-WHITE-100",
			},
		},
		api.VariantValue{
			ID:     2,
			Name:   "Orange",
			Swatch: "FF4500",
			SKUCodes: []string{
				"FREE-FLYKNIT-ORANGE-90",
				"FREE-FLYKNIT-ORANGE-100",
				"FREE-FABRIC-ORANGE-90",
				"FREE-FABRIC-ORANGE-100",
			},
		},
	}

	fabricValues := []api.VariantValue{
		api.VariantValue{
			ID:   3,
			Name: "Flyknit",
			SKUCodes: []string{
				"FREE-FLYKNIT-WHITE-90",
				"FREE-FLYKNIT-WHITE-100",
				"FREE-FLYKNIT-ORANGE-90",
				"FREE-FLYKNIT-ORANGE-100",
			},
		},
		api.VariantValue{
			ID:   4,
			Name: "Fabric",
			SKUCodes: []string{
				"FREE-FLYKNIT-WHITE-90",
				"FREE-FLYKNIT-WHITE-100",
				"FREE-FLYKNIT-ORANGE-90",
				"FREE-FLYKNIT-ORANGE-100",
			},
		},
	}

	sizeValues := []api.VariantValue{
		api.VariantValue{
			ID:   5,
			Name: "9.0",
			SKUCodes: []string{
				"FREE-FLYKNIT-WHITE-90",
				"FREE-FLYKNIT-ORANGE-90",
				"FREE-FABRIC-WHITE-90",
				"FREE-FABRIC-ORANGE-90",
			},
		},
		api.VariantValue{
			ID:   6,
			Name: "10.0",
			SKUCodes: []string{
				"FREE-FLYKNIT-WHITE-100",
				"FREE-FLYKNIT-ORANGE-100",
				"FREE-FABRIC-WHITE-100",
				"FREE-FABRIC-ORANGE-100",
			},
		},
	}

	colorVariant := makeVariant(12, "Color", "color", colorValues)
	sizeVariant := makeVariant(13, "Size", "size", sizeValues)
	fabricVariant := makeVariant(14, "Fabric", "fabric", fabricValues)

	return &api.Product{
		ID:         1,
		Context:    MakeContext(),
		Attributes: makeProductAttributes("Nike Free"),
		SKUs: []api.SKU{
			SKUs["FREE-FLYKNIT-ORANGE-90"],
			SKUs["FREE-FLYKNIT-ORANGE-100"],
			SKUs["FREE-FLYKNIT-WHITE-90"],
			SKUs["FREE-FLYKNIT-WHITE-100"],
			SKUs["FREE-FABRIC-ORANGE-90"],
			SKUs["FREE-FABRIC-ORANGE-100"],
			SKUs["FREE-FABRIC-WHITE-90"],
			SKUs["FREE-FABRIC-WHITE-100"],
		},
		Variants: []api.Variant{
			colorVariant,
			sizeVariant,
			fabricVariant,
		},
	}
}

func NewProductWithNoVariants() *api.Product {
	return &api.Product{
		ID:         1,
		Context:    MakeContext(),
		Attributes: makeProductAttributes("Nike Free Flyknit"),
		SKUs:       []api.SKU{SKUs["FREE-FLYKNIT-ORANGE-90"]},
	}
}

func makeProductAttributes(title string) api.ObjectAttributes {
	return api.ObjectAttributes{
		"title":       api.ObjectAttribute{Type: "string", Value: title},
		"activeTo":    api.ObjectAttribute{Type: "datetime", Value: ""},
		"activeFrom":  api.ObjectAttribute{Type: "datetime", Value: "2016-10-28T04:09:25.619Z"},
		"description": api.ObjectAttribute{Type: "richText", Value: "<p>The Nike Free Flyknit</p>"},
	}
}

var SKUs = map[string]api.SKU{
	"FREE-FLYKNIT-ORANGE-90": makeSKU(
		2,
		"FREE-FLYKNIT-ORANGE-90",
		"Nike Free Flyknit",
		13000,
		"https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/231/free-orange-flyknit.jpeg",
	),
	"FREE-FLYKNIT-ORANGE-100": makeSKU(
		3,
		"FREE-FLYKNIT-ORANGE-100",
		"Nike Free Flyknit",
		13000,
		"https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/231/free-orange-flyknit.jpeg",
	),
	"FREE-FLYKNIT-WHITE-90": makeSKU(
		4,
		"FREE-FLYKNIT-WHITE-90",
		"Nike Free Flyknit",
		13000,
		"https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/228/free-white-flyknit.jpeg",
	),
	"FREE-FLYKNIT-WHITE-100": makeSKU(
		5,
		"FREE-FLYKNIT-WHITE-100",
		"Nike Free Flyknit",
		13000,
		"https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/228/free-white-flyknit.jpeg",
	),
	"FREE-FABRIC-ORANGE-90": makeSKU(
		2,
		"FREE-FABRIC-ORANGE-90",
		"Nike Free Flyknit",
		13000,
		"https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/231/free-orange-fabric.jpeg",
	),
	"FREE-FABRIC-ORANGE-100": makeSKU(
		3,
		"FREE-FABRIC-ORANGE-100",
		"Nike Free Flyknit",
		13000,
		"https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/231/free-orange-fabric.jpeg",
	),
	"FREE-FABRIC-WHITE-90": makeSKU(
		4,
		"FREE-FABRIC-WHITE-90",
		"Nike Free Flyknit",
		13000,
		"https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/228/free-white-fabric.jpeg",
	),
	"FREE-FABRIC-WHITE-100": makeSKU(
		5,
		"FREE-FABRIC-WHITE-100",
		"Nike Free Flyknit",
		13000,
		"https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/228/free-white-fabric.jpeg",
	),
}

func makeSKU(id int, code string, title string, salePrice float64, imageURL string) api.SKU {
	attributes := api.ObjectAttributes{
		"code": api.ObjectAttribute{
			Type:  "string",
			Value: code,
		},
		"title": api.ObjectAttribute{
			Type:  "string",
			Value: title,
		},
		"salePrice": api.ObjectAttribute{
			Type: "price",
			Value: map[string]interface{}{
				"currency": "USD",
				"value":    salePrice,
			},
		},
		"activeTo": api.ObjectAttribute{
			Type:  "datetime",
			Value: "",
		},
		"activeFrom": api.ObjectAttribute{
			Type:  "datetime",
			Value: "2016-10-28T04:09:25.619Z",
		},
	}

	albums := []api.Album{
		api.Album{
			Name: "Default",
			Images: []api.Image{
				api.Image{Source: imageURL},
			},
		},
	}

	return api.SKU{
		ID:         id,
		Context:    MakeContext(),
		Attributes: attributes,
		Albums:     albums,
	}
}

func makeVariant(id int, name string, variantType string, values []api.VariantValue) api.Variant {
	return api.Variant{
		ID: id,
		Attributes: api.ObjectAttributes{
			"name": api.ObjectAttribute{Type: "string", Value: name},
			"type": api.ObjectAttribute{Type: "string", Value: variantType},
		},
		Values: values,
	}
}
