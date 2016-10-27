package main

import (
	"encoding/json"
	"fmt"

	"github.com/FoxComm/highlander/shared/golang/api"
)

const productJSON = `
{
  "attributes": {
    "externalUrl": {
      "t": "string",
      "v": "https://comrse.myshopify.com/products/shadow-mountain-dress"
    },
    "externalId": {
      "t": "string",
      "v": "455920540"
    },
    "description": {
      "t": "string",
      "v": "A springtime update to the Cooper LS Dress in Fall 14, the Shadow Mountain is a sexy take down of a men s athletic jerseycolor-blocked v-neck ringer and sleeve stripes with yoke panels.OBEY patch label at hem."
    },
    "title": {
      "t": "string",
      "v": "Shadow Mountain Dress"
    }
  },
  "variants": [
    {
      "attributes": {
      	"name": {
          "t": "string",
          "v": "Size"
      	}
      },
      "values": [
        {
          "name": "Small",
          "skuCodes": ["Small / Black", "Small / Red"]
        },
        {
          "name": "Medium",
          "skuCodes": ["Medium / Black", "Medium / Red"]
        },
        {
          "name": "Large",
          "skuCodes": ["Large / Black", "Large / Red"]
        }
      ]
    },
    {
      "attributes": {
        "name": {
    	  "t": "string",
    	  "v": "Color"
    	}
      },
      "values": [
        {
          "name": "Black",
          "skuCodes": ["Small / Black", "Medium / Black", "Large / Black"]
        },
        {
          "name": "Red",
          "skuCodes": ["Small / Red", "Medium / Red", "Large / Red"]
        }
      ]
    }
  ],
  "skus": [
    {
      "attributes": {
        "externalId": {
          "t": "string",
          "v": "1292233664"
        },
        "inventoryQuantity": {
          "t": "int",
          "v": "123"
        },
        "weight": {
          "t": "string",
          "v": "2.0"
        },
        "weightUnitOfMeasure": {
          "t": "string",
          "v": "lb"
        },
        "upc": {
          "t": "string",
          "v": ""
        },
        "code": {
          "t": "string",
          "v": "Small / Black"
        },
        "salePrice": {
          "t": "price",
          "v": {
            "value": 4500,
            "currency": "USD"
          }
        },
        "retailPrice": {
          "t": "price",
          "v": {
            "value": 4500,
            "currency": "USD"
          }
        }
      }
    },
    {
      "attributes": {
        "externalId": {
          "t": "string",
          "v": "1292233668"
        },
        "inventoryQuantity": {
          "t": "int",
          "v": "123"
        },
        "weight": {
          "t": "string",
          "v": "2.0"
        },
        "weightUnitOfMeasure": {
          "t": "string",
          "v": "lb"
        },
        "upc": {
          "t": "string",
          "v": ""
        },
        "code": {
          "t": "string",
          "v": "Small / Red"
        },
        "salePrice": {
          "t": "price",
          "v": {
            "value": 4500,
            "currency": "USD"
          }
        },
        "retailPrice": {
          "t": "price",
          "v": {
            "value": 4500,
            "currency": "USD"
          }
        }
      }
    },
    {
      "attributes": {
        "externalId": {
          "t": "string",
          "v": "1292233672"
        },
        "inventoryQuantity": {
          "t": "int",
          "v": "123"
        },
        "weight": {
          "t": "string",
          "v": "2.0"
        },
        "weightUnitOfMeasure": {
          "t": "string",
          "v": "lb"
        },
        "upc": {
          "t": "string",
          "v": ""
        },
        "code": {
          "t": "string",
          "v": "Medium / Black"
        },
        "salePrice": {
          "t": "price",
          "v": {
            "value": 4500,
            "currency": "USD"
          }
        },
        "retailPrice": {
          "t": "price",
          "v": {
            "value": 4500,
            "currency": "USD"
          }
        }
      }
    },
    {
      "attributes": {
        "externalId": {
          "t": "string",
          "v": "1292233676"
        },
        "inventoryQuantity": {
          "t": "int",
          "v": "123"
        },
        "weight": {
          "t": "string",
          "v": "2.0"
        },
        "weightUnitOfMeasure": {
          "t": "string",
          "v": "lb"
        },
        "upc": {
          "t": "string",
          "v": ""
        },
        "code": {
          "t": "string",
          "v": "Medium / Red"
        },
        "salePrice": {
          "t": "price",
          "v": {
            "value": 4500,
            "currency": "USD"
          }
        },
        "retailPrice": {
          "t": "price",
          "v": {
            "value": 4500,
            "currency": "USD"
          }
        }
      }
    },
    {
      "attributes": {
        "externalId": {
          "t": "string",
          "v": "1292233680"
        },
        "inventoryQuantity": {
          "t": "int",
          "v": "123"
        },
        "weight": {
          "t": "string",
          "v": "2.0"
        },
        "weightUnitOfMeasure": {
          "t": "string",
          "v": "lb"
        },
        "upc": {
          "t": "string",
          "v": ""
        },
        "code": {
          "t": "string",
          "v": "Large / Black"
        },
        "salePrice": {
          "t": "price",
          "v": {
            "value": 4500,
            "currency": "USD"
          }
        },
        "retailPrice": {
          "t": "price",
          "v": {
            "value": 4500,
            "currency": "USD"
          }
        }
      }
    },
    {
      "attributes": {
        "externalId": {
          "t": "string",
          "v": "1292233684"
        },
        "inventoryQuantity": {
          "t": "int",
          "v": "123"
        },
        "weight": {
          "t": "string",
          "v": "2.0"
        },
        "weightUnitOfMeasure": {
          "t": "string",
          "v": "lb"
        },
        "upc": {
          "t": "string",
          "v": ""
        },
        "code": {
          "t": "string",
          "v": "Large / Red"
        },
        "salePrice": {
          "t": "price",
          "v": {
            "value": 4500,
            "currency": "USD"
          }
        },
        "retailPrice": {
          "t": "price",
          "v": {
            "value": 4500,
            "currency": "USD"
          }
        }
      }
    }
  ],
  "albums": [
    {
      "name": "Shadow Mountain Dress Default Images",
      "images": [
        {
          "src": "https://cdn.shopify.com/s/files/1/0359/6249/products/ShadowMountainDressBLACK48.png?v=1428103704",
          "title": "Shadow Mountain Dress Image 1"
        },
        {
          "src": "https://cdn.shopify.com/s/files/1/0359/6249/products/ShadowMountainDressRED48.png?v=1428103704",
          "title": "Shadow Mountain Dress Image 2"
        }
      ]
    }
  ]
}
`

type VariantValueCombination struct {
	Variant string
	Value   string
}

type SelectedSKU struct {
	Code     string
	Variants []VariantValueCombination
}

//func getSelectedSKUs(vs map[string][]api.VariantValue) []SelectedSKU {
//for k, v := range vs {

//}
//return []SelectedSKU{}
//}

type SKUMapping struct {
	AvailableSKUs []string
	Variants      []VariantValueCombination
}

func stringersect(arr1 []string, arr2 []string) []string {
	inter := []string{}
	for _, a1 := range arr1 {
		for _, a2 := range arr2 {
			if a1 == a2 {
				inter = append(inter, a1)
			}
		}
	}

	return inter
}

func iterate(variants []api.Variant, state SKUMapping) []SKUMapping {
	tail := variants[1:]

	mappings := []SKUMapping{}
	variantName, err := variants[0].Name()
	if err != nil {
		panic(err)
	}

	if variantName != "Color" {
		return iterate(tail, state)
	}

	for _, value := range variants[0].Values {
		var nas []string
		if len(state.AvailableSKUs) == 0 {
			nas = value.SKUCodes
		} else {
			nas = stringersect(state.AvailableSKUs, value.SKUCodes)
		}

		newCombination := VariantValueCombination{Variant: variantName, Value: value.Name}
		newVariants := append(state.Variants, newCombination)

		mapping := SKUMapping{AvailableSKUs: nas, Variants: newVariants}

		if len(tail) == 0 {
			mappings = append(mappings, mapping)
		} else {
			newMappings := iterate(tail, mapping)
			mappings = append(mappings, newMappings...)
		}
	}

	return mappings
}

func main() {
	productByte := []byte(productJSON)
	product := new(api.Product)

	if err := json.Unmarshal(productByte, product); err != nil {
		panic(err)
	}

	s := SKUMapping{}
	a := iterate(product.Variants, s)
	for _, selected := range a {
		if len(selected.AvailableSKUs) == 0 {
			panic("Must not have 0 SKUs")
		}

		fmt.Printf("Code: %s\n", selected.AvailableSKUs[0])
		for _, variant := range selected.Variants {
			fmt.Printf("Variant: %s\n", variant.Variant)
			fmt.Printf("Value: %s\n", variant.Value)
		}

		fmt.Println("--------------------")
	}
}
