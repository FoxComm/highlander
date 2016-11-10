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
		Attributes: makeProductAttributes(),
		SKUs:       makeSKUs(),
		Variants: []api.Variant{
			colorVariant,
			sizeVariant,
		},
	}
}

func makeProductAttributes() api.ObjectAttributes {
	return api.ObjectAttributes{
		"title":       api.ObjectAttribute{Type: "string", Value: "Nike Free Flyknit"},
		"activeTo":    api.ObjectAttribute{Type: "datetime", Value: ""},
		"activeFrom":  api.ObjectAttribute{Type: "datetime", Value: "2016-10-28T04:09:25.619Z"},
		"description": api.ObjectAttribute{Type: "richText", Value: "<p>The Nike Free Flyknit</p>"},
	}
}

func makeSKUs() []api.SKU {
	return []api.SKU{
		makeSKU(
			2,
			"FREE-FLYKNIT-ORANGE-90",
			"Nike Free Flyknit",
			13000,
			"https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/231/free-orange-flyknit.jpeg",
		),
		makeSKU(
			3,
			"FREE-FLYKNIT-ORANGE-100",
			"Nike Free Flyknit",
			13000,
			"https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/231/free-orange-flyknit.jpeg",
		),
		makeSKU(
			4,
			"FREE-FLYKNIT-WHITE-90",
			"Nike Free Flyknit",
			13000,
			"https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/228/free-white-flyknit.jpeg",
		),
		makeSKU(
			5,
			"FREE-FLYKNIT-WHITE-100",
			"Nike Free Flyknit",
			13000,
			"https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/228/free-white-flyknit.jpeg",
		),
	}
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

const ProductNoVariants = `
{
  "id": 17,
  "context": {
    "name": "default",
    "attributes": {
      "lang": "en",
      "modality": "desktop"
    }
  },
  "attributes": {
    "tags": {
      "t": "tags",
      "v": [
        "sunglasses",
        "readers"
      ]
    },
    "title": {
      "t": "string",
      "v": "Duckling"
    },
    "activeFrom": {
      "t": "date",
      "v": "2016-10-28T03:22:25.006Z"
    },
    "description": {
      "t": "richText",
      "v": "A fit for a smaller face."
    }
  },
  "albums": [
    {
      "id": 19,
      "name": "Duckling",
      "images": [
        {
          "id": 5,
          "src": "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Marley_Top_Front.jpg",
          "title": "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Marley_Top_Front.jpg",
          "alt": "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Marley_Top_Front.jpg"
        }
      ],
      "createdAt": "2016-10-28T03:22:26.308Z",
      "updatedAt": "2016-10-28T03:22:26.308Z"
    }
  ],
  "skus": [
    {
      "id": 18,
      "attributes": {
        "code": {
          "t": "string",
          "v": "SKU-ZYA"
        },
        "tags": {
          "t": "tags",
          "v": [
            "sunglasses",
            "readers"
          ]
        },
        "title": {
          "t": "string",
          "v": "Duckling"
        },
        "salePrice": {
          "t": "price",
          "v": {
            "value": 8800,
            "currency": "USD"
          }
        },
        "activeFrom": {
          "t": "date",
          "v": "2016-10-28T03:22:26.180Z"
        },
        "retailPrice": {
          "t": "price",
          "v": {
            "value": 9300,
            "currency": "USD"
          }
        }
      },
      "albums": []
    }
  ],
  "variants": []
}
`

const ProductMultipleVisualVariants = `
{
  "id": 254,
  "context": {
    "name": "default",
    "attributes": {
      "lang": "en",
      "modality": "desktop"
    }
  },
  "attributes": {
    "title": {
      "t": "string",
      "v": "Nike Free"
    },
    "activeTo": {
      "t": "datetime",
      "v": null
    },
    "activeFrom": {
      "t": "datetime",
      "v": "2016-10-28T04:25:45.936Z"
    },
    "description": {
      "t": "richText",
      "v": "<p>All the Nikes our mine</p>"
    }
  },
  "albums": [],
  "skus": [
    {
      "id": 223,
      "context": {
        "name": "default",
        "attributes": {
          "lang": "en",
          "modality": "desktop"
        }
      },
      "attributes": {
        "code": {
          "t": "string",
          "v": "FREE-FLYKNIT-WHITE-90"
        },
        "title": {
          "t": "string",
          "v": "Nike Free"
        },
        "context": {
          "v": "default"
        },
        "activeTo": {
          "t": "datetime",
          "v": null
        },
        "salePrice": {
          "t": "price",
          "v": {
            "value": 13000,
            "currency": "USD"
          }
        },
        "activeFrom": {
          "t": "datetime",
          "v": "2016-10-28T04:25:45.936Z"
        },
        "retailPrice": {
          "t": "price",
          "v": {
            "value": 13000,
            "currency": "USD"
          }
        }
      },
      "albums": [
        {
          "id": 225,
          "name": "Default",
          "images": [
            {
              "id": 15,
              "src": "https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/225/free-white-flyknit.jpeg",
              "title": "free-white-flyknit.jpeg",
              "alt": "free-white-flyknit.jpeg"
            }
          ],
          "createdAt": "2016-10-28T04:03:09.850Z",
          "updatedAt": "2016-10-28T04:03:09.850Z"
        }
      ]
    },
    {
      "id": 242,
      "context": {
        "name": "default",
        "attributes": {
          "lang": "en",
          "modality": "desktop"
        }
      },
      "attributes": {
        "upc": {
          "t": "string",
          "v": ""
        },
        "code": {
          "t": "string",
          "v": "FREE-FABRIC-WHITE-90"
        },
        "title": {
          "t": "string",
          "v": "Nike Free"
        },
        "context": {
          "v": "default"
        },
        "activeTo": {
          "t": "datetime",
          "v": null
        },
        "unitCost": {
          "t": "price",
          "v": {
            "value": 0,
            "currency": "USD"
          }
        },
        "salePrice": {
          "t": "price",
          "v": {
            "value": 12000,
            "currency": "USD"
          }
        },
        "activeFrom": {
          "t": "datetime",
          "v": "2016-10-28T04:25:45.936Z"
        },
        "description": {
          "t": "richText",
          "v": ""
        },
        "retailPrice": {
          "t": "price",
          "v": {
            "value": 12000,
            "currency": "USD"
          }
        }
      },
      "albums": [
        {
          "id": 243,
          "name": "Default",
          "images": [
            {
              "id": 19,
              "src": "https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/243/free-white-fabric.jpeg",
              "title": "free-white-fabric.jpeg",
              "alt": "free-white-fabric.jpeg"
            }
          ],
          "createdAt": "2016-10-28T04:13:58.597Z",
          "updatedAt": "2016-10-28T04:13:58.597Z"
        }
      ]
    },
    {
      "id": 227,
      "context": {
        "name": "default",
        "attributes": {
          "lang": "en",
          "modality": "desktop"
        }
      },
      "attributes": {
        "upc": {
          "t": "string",
          "v": ""
        },
        "code": {
          "t": "string",
          "v": "FREE-FLYKNIT-WHITE-100"
        },
        "title": {
          "t": "string",
          "v": "Nike Free"
        },
        "context": {
          "v": "default"
        },
        "activeTo": {
          "t": "datetime",
          "v": null
        },
        "unitCost": {
          "t": "price",
          "v": {
            "value": 0,
            "currency": "USD"
          }
        },
        "salePrice": {
          "t": "price",
          "v": {
            "value": 13000,
            "currency": "USD"
          }
        },
        "activeFrom": {
          "t": "datetime",
          "v": "2016-10-28T04:25:45.936Z"
        },
        "description": {
          "t": "richText",
          "v": ""
        },
        "retailPrice": {
          "t": "price",
          "v": {
            "value": 13000,
            "currency": "USD"
          }
        }
      },
      "albums": [
        {
          "id": 228,
          "name": "Default",
          "images": [
            {
              "id": 16,
              "src": "https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/228/free-white-flyknit.jpeg",
              "title": "free-white-flyknit.jpeg",
              "alt": "free-white-flyknit.jpeg"
            }
          ],
          "createdAt": "2016-10-28T04:05:36.809Z",
          "updatedAt": "2016-10-28T04:05:36.809Z"
        }
      ]
    },
    {
      "id": 245,
      "context": {
        "name": "default",
        "attributes": {
          "lang": "en",
          "modality": "desktop"
        }
      },
      "attributes": {
        "upc": {
          "t": "string",
          "v": ""
        },
        "code": {
          "t": "string",
          "v": "FREE-FABRIC-WHITE-100"
        },
        "title": {
          "t": "string",
          "v": "Nike Free"
        },
        "context": {
          "v": "default"
        },
        "activeTo": {
          "t": "datetime",
          "v": null
        },
        "unitCost": {
          "t": "price",
          "v": {
            "value": 0,
            "currency": "USD"
          }
        },
        "salePrice": {
          "t": "price",
          "v": {
            "value": 12000,
            "currency": "USD"
          }
        },
        "activeFrom": {
          "t": "datetime",
          "v": "2016-10-28T04:25:45.936Z"
        },
        "description": {
          "t": "richText",
          "v": ""
        },
        "retailPrice": {
          "t": "price",
          "v": {
            "value": 12000,
            "currency": "USD"
          }
        }
      },
      "albums": [
        {
          "id": 246,
          "name": "Default",
          "images": [
            {
              "id": 20,
              "src": "https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/246/free-white-fabric.jpeg",
              "title": "free-white-fabric.jpeg",
              "alt": "free-white-fabric.jpeg"
            }
          ],
          "createdAt": "2016-10-28T04:17:25.169Z",
          "updatedAt": "2016-10-28T04:17:25.169Z"
        }
      ]
    },
    {
      "id": 233,
      "context": {
        "name": "default",
        "attributes": {
          "lang": "en",
          "modality": "desktop"
        }
      },
      "attributes": {
        "upc": {
          "t": "string",
          "v": ""
        },
        "code": {
          "t": "string",
          "v": "FREE-FLYKNIT-ORANGE-100"
        },
        "title": {
          "t": "string",
          "v": "Nike Free"
        },
        "context": {
          "v": "default"
        },
        "activeTo": {
          "t": "datetime",
          "v": null
        },
        "unitCost": {
          "t": "price",
          "v": {
            "value": 0,
            "currency": "USD"
          }
        },
        "salePrice": {
          "t": "price",
          "v": {
            "value": 13000,
            "currency": "USD"
          }
        },
        "activeFrom": {
          "t": "datetime",
          "v": "2016-10-28T04:25:45.936Z"
        },
        "description": {
          "t": "richText",
          "v": ""
        },
        "retailPrice": {
          "t": "price",
          "v": {
            "value": 13000,
            "currency": "USD"
          }
        }
      },
      "albums": [
        {
          "id": 234,
          "name": "Default",
          "images": [
            {
              "id": 18,
              "src": "https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/234/free-orange-flyknit.jpeg",
              "title": "free-orange-flyknit.jpeg",
              "alt": "free-orange-flyknit.jpeg"
            }
          ],
          "createdAt": "2016-10-28T04:07:24.120Z",
          "updatedAt": "2016-10-28T04:07:24.120Z"
        }
      ]
    },
    {
      "id": 230,
      "context": {
        "name": "default",
        "attributes": {
          "lang": "en",
          "modality": "desktop"
        }
      },
      "attributes": {
        "upc": {
          "t": "string",
          "v": ""
        },
        "code": {
          "t": "string",
          "v": "FREE-FLYKNIT-ORANGE-90"
        },
        "title": {
          "t": "string",
          "v": "Nike Free"
        },
        "context": {
          "v": "default"
        },
        "activeTo": {
          "t": "datetime",
          "v": null
        },
        "unitCost": {
          "t": "price",
          "v": {
            "value": 0,
            "currency": "USD"
          }
        },
        "salePrice": {
          "t": "price",
          "v": {
            "value": 13000,
            "currency": "USD"
          }
        },
        "activeFrom": {
          "t": "datetime",
          "v": "2016-10-28T04:25:45.936Z"
        },
        "description": {
          "t": "richText",
          "v": ""
        },
        "retailPrice": {
          "t": "price",
          "v": {
            "value": 13000,
            "currency": "USD"
          }
        }
      },
      "albums": [
        {
          "id": 231,
          "name": "Default",
          "images": [
            {
              "id": 17,
              "src": "https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/231/free-orange-flyknit.jpeg",
              "title": "free-orange-flyknit.jpeg",
              "alt": "free-orange-flyknit.jpeg"
            }
          ],
          "createdAt": "2016-10-28T04:06:42.285Z",
          "updatedAt": "2016-10-28T04:06:42.285Z"
        }
      ]
    },
    {
      "id": 248,
      "context": {
        "name": "default",
        "attributes": {
          "lang": "en",
          "modality": "desktop"
        }
      },
      "attributes": {
        "upc": {
          "t": "string",
          "v": ""
        },
        "code": {
          "t": "string",
          "v": "FREE-FABRIC-ORANGE-90"
        },
        "title": {
          "t": "string",
          "v": "Nike Free"
        },
        "context": {
          "v": "default"
        },
        "activeTo": {
          "t": "datetime",
          "v": null
        },
        "unitCost": {
          "t": "price",
          "v": {
            "value": 0,
            "currency": "USD"
          }
        },
        "salePrice": {
          "t": "price",
          "v": {
            "value": 12000,
            "currency": "USD"
          }
        },
        "activeFrom": {
          "t": "datetime",
          "v": "2016-10-28T04:25:45.936Z"
        },
        "description": {
          "t": "richText",
          "v": ""
        },
        "retailPrice": {
          "t": "price",
          "v": {
            "value": 12000,
            "currency": "USD"
          }
        }
      },
      "albums": [
        {
          "id": 249,
          "name": "Default",
          "images": [
            {
              "id": 21,
              "src": "https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/249/free-orange-fabrick.jpg",
              "title": "free-orange-fabrick.jpg",
              "alt": "free-orange-fabrick.jpg"
            }
          ],
          "createdAt": "2016-10-28T04:18:05.636Z",
          "updatedAt": "2016-10-28T04:18:05.636Z"
        }
      ]
    }
  ],
  "variants": [
    {
      "id": 255,
      "attributes": {
        "name": {
          "t": "string",
          "v": "Color"
        },
        "type": {
          "t": "string",
          "v": "color"
        }
      },
      "values": [
        {
          "id": 256,
          "name": "White",
          "swatch": "ffffff",
          "skuCodes": [
            "FREE-FLYKNIT-WHITE-90",
            "FREE-FABRIC-WHITE-90",
            "FREE-FLYKNIT-WHITE-100",
            "FREE-FABRIC-WHITE-100"
          ]
        },
        {
          "id": 257,
          "name": "Orange",
          "swatch": "FF4500",
          "skuCodes": [
            "FREE-FLYKNIT-ORANGE-90",
            "FREE-FABRIC-ORANGE-90",
            "FREE-FLYKNIT-ORANGE-100",
            "FREE-FLYKNIT-ORANGE-100"
          ]
        }
      ]
    },
    {
      "id": 258,
      "attributes": {
        "name": {
          "t": "string",
          "v": "Fabric"
        },
        "type": {
          "t": "string",
          "v": "fabric"
        }
      },
      "values": [
        {
          "id": 259,
          "name": "Flyknit",
          "swatch": "",
          "skuCodes": [
            "FREE-FLYKNIT-WHITE-90",
            "FREE-FLYKNIT-ORANGE-90",
            "FREE-FLYKNIT-WHITE-100",
            "FREE-FLYKNIT-ORANGE-100"
          ]
        },
        {
          "id": 260,
          "name": "Cloth",
          "swatch": "",
          "skuCodes": [
            "FREE-FABRIC-WHITE-90",
            "FREE-FABRIC-ORANGE-90",
            "FREE-FABRIC-WHITE-100",
            "FREE-FLYKNIT-ORANGE-100"
          ]
        }
      ]
    },
    {
      "id": 261,
      "attributes": {
        "name": {
          "t": "string",
          "v": "Size"
        },
        "type": {
          "t": "string",
          "v": "size"
        }
      },
      "values": [
        {
          "id": 262,
          "name": "9.0",
          "swatch": "",
          "skuCodes": [
            "FREE-FLYKNIT-WHITE-90",
            "FREE-FLYKNIT-ORANGE-90",
            "FREE-FABRIC-WHITE-90",
            "FREE-FABRIC-ORANGE-90"
          ]
        },
        {
          "id": 263,
          "name": "10.0",
          "swatch": "",
          "skuCodes": [
            "FREE-FLYKNIT-WHITE-100",
            "FREE-FABRIC-WHITE-100",
            "FREE-FLYKNIT-ORANGE-100",
            "FREE-FLYKNIT-ORANGE-100"
          ]
        }
      ]
    }
  ]
}
`
