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
}
