package controllers

import (
	"net/http"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/services"

	"github.com/FoxComm/middlewarehouse/common/failures"
	"github.com/gin-gonic/gin"
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
			fail := failures.NewInternalError(err)
			failures.Abort(context, fail)
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
		id, fail := paramUint(context, "id")
		if fail != nil {
			return
		}

		stockItem, err := controller.service.GetStockItemById(uint(id))
		if err != nil {
			handleServiceError(context, err)
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
			handleServiceError(context, err)
			return
		}

		resp := responses.NewStockItemFromModel(stockItem)

		context.JSON(http.StatusCreated, resp)
	}
}

func (controller *stockItemController) IncrementStockItemUnits() gin.HandlerFunc {
	return func(context *gin.Context) {
		id, fail := paramUint(context, "id")
		if fail != nil {
			return
		}

		var payload payloads.IncrementStockItemUnits
		if parse(context, &payload) != nil {
			return
		}

		if err := payload.Validate(); err != nil {
			failures.Abort(context, failures.NewBadRequest(err))
			return
		}

		units := models.NewStockItemUnitsFromPayload(uint(id), &payload)

		if err := controller.service.IncrementStockItemUnits(uint(id), units); err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusCreated, gin.H{})
	}
}

func (controller *stockItemController) DecrementStockItemUnits() gin.HandlerFunc {
	return func(context *gin.Context) {
		id, fail := paramUint(context, "id")
		if fail != nil {
			return
		}

		var payload payloads.DecrementStockItemUnits
		if parse(context, &payload) != nil {
			return
		}

		if err := payload.Validate(); err != nil {
			failures.Abort(context, failures.NewBadRequest(err))
			return
		}

		err := controller.service.DecrementStockItemUnits(uint(id), payload.Qty)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusCreated, gin.H{})
	}
}
