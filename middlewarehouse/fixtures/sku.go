package fixtures

import "github.com/FoxComm/highlander/middlewarehouse/models"

func GetSKU() *models.SKU {
	return &models.SKU{
		Scope:                              "1",
		Code:                               "SKU-TEST",
		UPC:                                "12312342131",
		Title:                              "Some test SKU",
		UnitCost:                           299,
		TaxClass:                           "default",
		RequiresShipping:                   true,
		ShippingClass:                      "default",
		IsReturnable:                       true,
		ReturnWindowValue:                  30.0,
		ReturnWindowUnits:                  "days",
		HeightValue:                        15.0,
		HeightUnits:                        "cm",
		LengthValue:                        10.0,
		LengthUnits:                        "cm",
		WidthValue:                         5.0,
		WidthUnits:                         "cm",
		WeightValue:                        50.0,
		WeightUnits:                        "g",
		RequiresInventoryTracking:          true,
		InventoryWarningLevelIsEnabled:     true,
		InventoryWarningLevelValue:         3,
		MaximumQuantityInCartIsEnabled:     true,
		MaximumQuantityInCartValue:         6,
		MinimumQuantityInCartIsEnabled:     false,
		AllowPreorder:                      false,
		AllowBackorder:                     false,
		RequiresLotTracking:                true,
		LotExpirationThresholdValue:        3.0,
		LotExpirationThresholdUnits:        "months",
		LotExpirationWarningThresholdValue: 15.0,
		LotExpirationWarningThresholdUnits: "days",
	}
}
