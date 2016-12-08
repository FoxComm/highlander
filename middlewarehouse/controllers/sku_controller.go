package controllers

import (
	"net/http"

	"github.com/gin-gonic/gin"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
)

type skuController struct{}

func NewSKUController() IController {
	return &skuController{}
}

func (controller *skuController) SetUp(router gin.IRouter) {
	router.Use(FetchJWT)
	router.GET(":id", controller.GetSKUByID())
}

func (controller *skuController) GetSKUByID() gin.HandlerFunc {
	return func(context *gin.Context) {
		resp := responses.SKU{
			ID:               1,
			Code:             "SKU-TEST",
			UPC:              "12312342131",
			Title:            "Some test SKU",
			UnitCost:         299,
			TaxClass:         "default",
			RequiresShipping: true,
			ShippingClass:    "default",
			IsReturnable:     true,
			ReturnWindow: responses.Dimension{
				Value: 30.0,
				Units: "days",
			},
			Height: responses.Dimension{
				Value: 15.0,
				Units: "cm",
			},
			Length: responses.Dimension{
				Value: 10.0,
				Units: "cm",
			},
			Width: responses.Dimension{
				Value: 5.0,
				Units: "cm",
			},
			Weight: responses.Dimension{
				Value: 50.0,
				Units: "g",
			},
			RequiresInventoryTracking: true,
			InventoryWarningLevel: responses.QuantityLevel{
				IsEnabled: true,
				Level:     3,
			},
			MaximumQuantityInCart: responses.QuantityLevel{
				IsEnabled: true,
				Level:     6,
			},
			MinimumQuantityInCart: responses.QuantityLevel{
				IsEnabled: false,
			},
			AllowPreorder:       false,
			AllowBackorder:      false,
			RequiresLotTracking: true,
			LotExpirationThreshold: responses.Dimension{
				Value: 3.0,
				Units: "months",
			},
			LotExpirationWarningThreshold: responses.Dimension{
				Value: 15.0,
				Units: "days",
			},
		}
		context.JSON(http.StatusOK, resp)
	}
}
