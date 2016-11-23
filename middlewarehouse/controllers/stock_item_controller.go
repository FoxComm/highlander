package controllers

import (
	"net/http"
	"strconv"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/failures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/services"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/gin-gonic/gin"
)

type stockItemController struct {
	service services.IInventoryService
}

func NewStockItemController(service services.IInventoryService) IController {
	return &stockItemController{service}
}

func (controller *stockItemController) SetUp(router gin.IRouter) {
	router.GET("", controller.GetStockItems())
	router.GET(":id", controller.GetStockItemById())
	router.POST("", controller.CreateStockItem())
	router.PATCH(":id/increment", controller.IncrementStockItemUnits())
	router.PATCH(":id/decrement", controller.DecrementStockItemUnits())

	router.GET(":id/afs/:type", controller.GetAFS())
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

		context.JSON(http.StatusCreated, responses.NewStockItemFromModel(stockItem))
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

		if err := controller.service.IncrementStockItemUnits(uint(id), models.UnitType(payload.Type), units); err != nil {
			handleServiceError(context, err)
			return
		}

		context.Status(http.StatusNoContent)
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
		if err := controller.service.DecrementStockItemUnits(uint(id), models.UnitType(payload.Type), payload.Qty); err != nil {
			handleServiceError(context, err)
			return
		}

		context.Status(http.StatusNoContent)
	}
}

func (controller *stockItemController) GetAFS() gin.HandlerFunc {
	return func(context *gin.Context) {
		idOrSKUStr := context.Params.ByName("id")
		unitType := context.Params.ByName("type")

		// trying to convert idOrSKU string to int
		idOrSKU, err := strconv.Atoi(idOrSKUStr)

		afs := &models.AFS{}
		var exception exceptions.IException
		if err == nil {
			// if successfully converted to int try to find by ID
			afs, exception = controller.service.GetAFSByID(uint(idOrSKU), models.UnitType(unitType))
		} else {
			// trying find by sku code otherwise
			afs, exception = controller.service.GetAFSBySKU(idOrSKUStr, models.UnitType(unitType))
		}

		if exception != nil {
			handleServiceError(context, exception)
			return
		}

		response := responses.NewAFSFromModel(afs)

		context.JSON(200, response)
	}
}
