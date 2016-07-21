package controllers

import (
	"strconv"
	"net/http"

	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/services"
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
)

type stockItemController struct {
	service services.IInventoryService
}

func NewStockItemController(service services.IInventoryService) IController {
	return &stockItemController{service}
}

func (controller *stockItemController) SetUp(router gin.IRouter) {
	router.GET("/", controller.GetStockItems())
	router.GET("/:id", controller.GetStockItemById())
	router.POST("/", controller.CreateStockItem())
	router.PATCH("/:id/increment", controller.IncrementStockItemUnits())
	router.PATCH("/:id/decrement", controller.DecrementStockItemUnits())
}

func (controller *stockItemController) GetStockItems() gin.HandlerFunc {
	return func(context *gin.Context) {
		stockItems, err := controller.service.GetStockItems()

		if err != nil {
			context.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		response := make([]*responses.StockItem, len(stockItems))
		for i := range stockItems {
			response[i] = responses.NewStockItemFromModel(stockItems[i])
		}

		context.JSON(http.StatusOK, response)
	}
}

func (controller *stockItemController) GetStockItemById() gin.HandlerFunc {
	return func(context *gin.Context) {
		idStr := context.Params.ByName("id")

		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			context.AbortWithError(http.StatusBadRequest, err)
			return
		}

		stockItem, err := controller.service.GetStockItemByID(uint(id))
		if err != nil {
			if err == gorm.ErrRecordNotFound {
				context.AbortWithStatus(http.StatusNotFound)
			} else {
				context.AbortWithError(http.StatusInternalServerError, err)
			}
			return
		}

		resp := responses.NewStockItemFromModel(stockItem)

		context.JSON(http.StatusOK, resp)
	}
}

func (controller *stockItemController) CreateStockItem() gin.HandlerFunc {
	return func(context *gin.Context) {
		var payload payloads.StockItem
		if parse(context, &payload) != nil {
			return
		}

		stockItem := models.NewStockItemFromPayload(&payload)

		stockItem, err := controller.service.CreateStockItem(stockItem)
		if err != nil {
			context.AbortWithError(http.StatusBadRequest, err)
			return
		}

		resp := responses.NewStockItemFromModel(stockItem)

		context.JSON(http.StatusCreated, resp)
	}
}

func (controller *stockItemController) IncrementStockItemUnits() gin.HandlerFunc {
	return func(context *gin.Context) {
		id, err := strconv.ParseUint(context.Params.ByName("id"), 10, 64)
		if err != nil {
			context.AbortWithError(http.StatusBadRequest, err)
			return
		}

		var payload payloads.IncrementStockItemUnits
		if parse(context, &payload) != nil {
			return
		}

		if err := payload.Validate(); err != nil {
			context.AbortWithError(http.StatusBadRequest, err)
			return
		}

		units := models.NewStockItemUnitsFromPayload(uint(id), &payload)

		if err := controller.service.IncrementStockItemUnits(uint(id), units); err != nil {
			context.AbortWithError(http.StatusBadRequest, err)
			return
		}

		context.JSON(http.StatusCreated, gin.H{})
	}
}

func (controller *stockItemController) DecrementStockItemUnits() gin.HandlerFunc {
	return func(context *gin.Context) {
		id, err := strconv.ParseUint(context.Params.ByName("id"), 10, 64)
		if err != nil {
			context.AbortWithError(http.StatusBadRequest, err)
			return
		}

		var payload payloads.DecrementStockItemUnits
		if parse(context, &payload) != nil {
			return
		}

		if err := payload.Validate(); err != nil {
			context.AbortWithError(http.StatusBadRequest, err)
			return
		}

		err = controller.service.DecrementStockItemUnits(uint(id), payload.Qty)
		if err != nil {
			context.AbortWithError(http.StatusBadRequest, err)
			return
		}

		context.Status(http.StatusNoContent)
	}
}
