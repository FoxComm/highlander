package controllers

import (
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/services"
	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
)

type bulkActionsController struct {
	skuService services.SKU
}

func NewBulkActionsController(db *gorm.DB) IController {
	skuService := services.NewSKU(db)
	return &bulkActionsController{skuService}
}

func (controller *bulkActionsController) SetUp(router gin.IRouter) {
	router.Use(FetchJWT)
	router.POST("skus", controller.CreateSKUs())
}

func (controller *bulkActionsController) CreateSKUs() gin.HandlerFunc {
	return func(context *gin.Context) {
		payload := []*payloads.CreateSKU{}
		if parse(context, &payload) != nil {
			return
		}

		for _, cs := range payload {
			if !setScope(context, cs) {
				return
			}
		}

		resp, err := controller.skuService.CreateBulk(payload)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusCreated, resp)
	}
}
