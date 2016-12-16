package fixtures

import (
	"strconv"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/icrowley/fake"
)

func randomBool() bool {
	num, _ := strconv.Atoi(fake.DigitsN(1))
	return num < 5
}

func randomFloat() float64 {
	num, _ := strconv.ParseFloat(fake.Digits(), 64)
	return num
}

func randomInt() int {
	num, _ := strconv.Atoi(fake.DigitsN(3))
	return num
}

func GetCreateSKUPayload() *payloads.CreateSKU {
	return &payloads.CreateSKU{
		Code:             fake.CharactersN(10),
		UPC:              fake.CharactersN(15),
		Title:            fake.ProductName(),
		UnitCost:         randomInt(),
		TaxClass:         "default",
		RequiresShipping: randomBool(),
		ShippingClass:    "default",
		IsReturnable:     randomBool(),
		ReturnWindow: &payloads.PhysicalUnit{
			Value: randomFloat(),
			Units: "days",
		},
		Height: &payloads.PhysicalUnit{
			Value: randomFloat(),
			Units: "cm",
		},
		Length: &payloads.PhysicalUnit{
			Value: randomFloat(),
			Units: "cm",
		},
		Width: &payloads.PhysicalUnit{
			Value: randomFloat(),
			Units: "cm",
		},
		Weight: &payloads.PhysicalUnit{
			Value: randomFloat(),
			Units: "g",
		},
		RequiresInventoryTracking: randomBool(),
		InventoryWarningLevel: &payloads.QuantityLevel{
			IsEnabled: randomBool(),
			Level:     randomInt(),
		},
		MaximumQuantityInCart: &payloads.QuantityLevel{
			IsEnabled: randomBool(),
			Level:     randomInt(),
		},
		MinimumQuantityInCart: &payloads.QuantityLevel{
			IsEnabled: randomBool(),
			Level:     randomInt(),
		},
		AllowPreorder:       randomBool(),
		AllowBackorder:      randomBool(),
		RequiresLotTracking: randomBool(),
		LotExpirationThreshold: &payloads.PhysicalUnit{
			Value: randomFloat(),
			Units: "months",
		},
		LotExpirationWarningThreshold: &payloads.PhysicalUnit{
			Value: randomFloat(),
			Units: "days",
		},
	}
}

func GetSKU() *models.SKU {
	sku := GetCreateSKUPayload().Model()
	sku.Scope = "1"
	return sku
}
