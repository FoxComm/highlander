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

func main() {
	productByte := []byte(productJSON)
	product := new(api.Product)

	if err := json.Unmarshal(productByte, product); err != nil {
		panic(err)
	}

	pp := PartialProduct{}
	vs := []string{"Color"}

	prods, err := MakePartialProducts(product.Variants, pp, vs)
	if err != nil {
		panic(err)
	}

	for _, prod := range prods {
		row, err := NewSearchRow(*product, prod)
		if err != nil {
			panic(err)
		}

		fmt.Printf("Codes: %q\n", row.SKUs)
		fmt.Printf("Context: %s\n", row.Context)
		fmt.Printf("Title: %s\n", row.Title)
		fmt.Printf("Description: %s\n", row.Description)
		fmt.Printf("Image: %s\n", row.Image)
		fmt.Printf("Sale Price: %d\n", row.SalePrice)
		fmt.Printf("Currency: %s\n", row.Currency)
		for variant, value := range prod.Variants {
			fmt.Printf("Variant: %s\n", variant)
			fmt.Printf("Value: %s\n", value)
		}

		fmt.Println("--------------------")
	}
}
