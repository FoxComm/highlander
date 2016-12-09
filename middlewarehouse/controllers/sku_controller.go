package controllers

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/services"
)

type skuController struct {
	skuService services.SKU
}

func NewSKUController(db *gorm.DB) IController {
	skuService := services.NewSKU(db)
	return &skuController{skuService}
}

func (controller *skuController) SetUp(router gin.IRouter) {
	router.Use(FetchJWT)
	router.POST("", controller.CreateSKU())
	router.GET(":id", controller.GetSKUByID())
	router.PATCH(":id", controller.UpdateSKU())
	router.GET(":id/afs", controller.GetAFS())
}

func (controller *skuController) CreateSKU() gin.HandlerFunc {
	return func(context *gin.Context) {
		resp := dummyResponse()
		context.JSON(http.StatusCreated, resp)
	}
}

func (controller *skuController) GetSKUByID() gin.HandlerFunc {
	return func(context *gin.Context) {
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		resp, err := controller.skuService.GetByID(id)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusOK, resp)
	}
}

func (controller *skuController) UpdateSKU() gin.HandlerFunc {
	return func(context *gin.Context) {
		resp := dummyResponse()
		context.JSON(http.StatusOK, resp)
	}
}

func (controller *skuController) GetAFS() gin.HandlerFunc {
	return func(context *gin.Context) {
		resp := map[string]int{"afs": 10}
		context.JSON(http.StatusOK, resp)
	}
}

func dummyResponse() *responses.SKU {
	model := &models.SKU{
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

	return responses.NewSKUFromModel(model)
}
