package main

import (
	"fmt"
	"os"

	"github.com/FoxComm/shipstation/lib"
	"github.com/FoxComm/shipstation/lib/payloads"
	_ "github.com/jpfuentes2/go-env/autoload"
)

func main() {
	key := os.Getenv("API_KEY")
	secret := os.Getenv("API_SECRET")

	client, err := lib.NewClient(key, secret)
	if err != nil {
		panic(err)
	}

	products, err := client.Products()
	if err != nil {
		panic(err)
	}

	fmt.Printf("Products: %v\n", products.Products)

	product, err := client.Product(products.Products[0].ID)
	if err != nil {
		panic(err)
	}

	fmt.Printf("Product: %v\n", product)

	payload := new(payloads.Product)
	payload.FromResponse(product)
	payload.Name = "Sharkling"

	update, err := client.UpdateProduct(payload)
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	fmt.Printf("Update: %v\n", update)

	fmt.Printf("Creating an Order\n")

	street2 := "Suite 320"
	phone := "2069637392"
	email := "jeff@foxcommerce.com"

	a := payloads.Address{
		Name:        "Jeff Mataya",
		Street1:     "2101 4th Ave",
		Street2:     &street2,
		City:        "Seattle",
		State:       "WA",
		PostalCode:  "98121",
		Country:     "US",
		Phone:       &phone,
		Residential: false,
	}

	o := payloads.Order{
		OrderNumber:   "BR10001",
		OrderDate:     "2016-07-05T00:00:00.0000000",
		OrderStatus:   "awaiting_shipment",
		CustomerEmail: &email,
		BillTo:        a,
		ShipTo:        a,
		Items: []payloads.OrderItem{
			payloads.OrderItem{
				SKU:  "SHARK",
				Name: "Shark (Line Item)",
				Weight: payloads.Weight{
					Value: 24,
					Units: "ounces",
				},
				Quantity: 1,
			},
		},
		Weight: payloads.Weight{
			Value: 25,
			Units: "ounces",
		},
	}

	out, err := client.CreateOrder(&o)
	if err != nil {
		panic(err)
	}

	fmt.Printf("Order: %v\n", out)
}
