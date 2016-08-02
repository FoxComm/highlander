package controllers

import (
	"net/http"

	// "github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/services"
	"github.com/gin-gonic/gin"
	"github.com/FoxComm/middlewarehouse/api/responses"
)

type shipmentController struct {
	shipmentService            services.IShipmentService
	addressService             services.IAddressService
	shipmentLineItemService    services.IShipmentLineItemService
	shipmentTransactionService services.IShipmentTransactionService
}

func NewShipmentController(
	shipmentService services.IShipmentService,
	addressService services.IAddressService,
	shipmentLineItemService services.IShipmentLineItemService,
	shipmentTransactionService services.IShipmentTransactionService,
) IController {
	return &shipmentController{shipmentService, addressService, shipmentLineItemService, shipmentTransactionService}
}

func (controller *shipmentController) SetUp(router gin.IRouter) {
	// router.GET("/", controller.getShipments())
	router.GET("/:id", controller.getShipmentByID())
	// router.POST("/", controller.createShipment())
}

func (controller *shipmentController) getShipmentByID() gin.HandlerFunc {
	return func(context *gin.Context) {
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		shipment, err := controller.shipmentService.GetShipmentByID(id)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		address, err := controller.addressService.GetAddressByID(shipment.AddressID)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		 shipmentLineItems, err := controller.shipmentLineItemService.GetShipmentLineItemsByShipmentID(id)
		 if err != nil {
		 	handleServiceError(context, err)
		 	return
		 }

		shipmentTransactions, err := controller.shipmentTransactionService.GetShipmentTransactionsByShipmentID(id)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		response := responses.NewShipmentFromModel(shipment)
		response.Address = *responses.NewAddressFromModel(address)
		for _, lineItem := range shipmentLineItems {
			response.LineItems = append(response.LineItems, *responses.NewShipmentLineItemFromModel(lineItem))
		}
		response.Transactions = *responses.NewTransactionListFromModelsList(shipmentTransactions)


		context.JSON(http.StatusOK, response)
	}
}

// func (controller *shipmentController) createShipment() gin.HandlerFunc {
// 	return func(context *gin.Context) {
// 		var payload payloads.Shipment
// 		if parse(context, &payload) != nil {
// 			return
// 		}

// 		resp, err := controller.service.CreateShipment(payload)
// 		if err != nil {
// 			handleServiceError(context, err)
// 			return
// 		}

// 		context.JSON(http.StatusOK, resp)
// 	}
// }
