package controllers

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
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
	router.DELETE(":id", controller.DeleteSKU())
	router.GET(":id/afs", controller.GetAFS())
}

func (controller *skuController) CreateSKU() gin.HandlerFunc {
	return func(context *gin.Context) {
		payload := &payloads.CreateSKU{}
		if parse(context, payload) != nil {
			return
		}

		if !setScope(context, payload) {
			return
		}

		resp, err := controller.skuService.Create(payload)
		if err != nil {
			handleServiceError(context, err)
			return
		}

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
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		payload := &payloads.UpdateSKU{}
		if parse(context, payload) != nil {
			return
		}

		resp, err := controller.skuService.Update(id, payload)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusOK, resp)
	}
}

func (controller *skuController) DeleteSKU() gin.HandlerFunc {
	return func(context *gin.Context) {
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		if err := controller.skuService.Delete(id); err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusNoContent, gin.H{})
	}
}

func (controller *skuController) GetAFS() gin.HandlerFunc {
	return func(context *gin.Context) {
		resp := map[string]int{"afs": 10}
		context.JSON(http.StatusOK, resp)
	}
}
