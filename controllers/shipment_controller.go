package controllers

import (
	"net/http"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/services"

	"github.com/gin-gonic/gin"
)

type shipmentController struct {
	shipmentService            services.IShipmentService
	addressService             services.IAddressService
	shipmentLineItemService    services.IShipmentLineItemService
	//shipmentTransactionService services.IShipmentTransactionService
}

func NewShipmentController(
	shipmentService services.IShipmentService,
	addressService services.IAddressService,
	shipmentLineItemService services.IShipmentLineItemService,
	//shipmentTransactionService services.IShipmentTransactionService,
) IController {
	return &shipmentController{shipmentService, addressService, shipmentLineItemService/*, shipmentTransactionService*/}
}

func (controller *shipmentController) SetUp(router gin.IRouter) {
	// router.GET("/", controller.getShipments())
	router.GET("/:id", controller.getShipmentByID())
	router.POST("/", controller.createShipment())
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

		response, err := controller.getShipmentResponse(shipment)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusOK, response)
	}
}

func (controller *shipmentController) createShipment() gin.HandlerFunc {
	return func(context *gin.Context) {
		payload := &payloads.Shipment{}
		if parse(context, payload) != nil {
			return
		}

		shipmentLineItems := make([]*models.ShipmentLineItem, len(payload.LineItems))
		for i, shipmentLineItemPayload := range payload.LineItems {
			shipmentLineItems[i] = models.NewShipmentLineItemFromPayload(&shipmentLineItemPayload)
		}
		shipment, err := controller.shipmentService.CreateShipment(
			models.NewShipmentFromPayload(payload),
			models.NewAddressFromPayload(&payload.Address),
			shipmentLineItems,
		)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		response, err := controller.getShipmentResponse(shipment)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusCreated, response)
	}
}

func (controller *shipmentController) getShipmentResponse(shipment *models.Shipment) (*responses.Shipment, error) {
	address, err := controller.addressService.GetAddressByID(shipment.AddressID)
	if err != nil {
		return nil, err
	}

	shipmentLineItems, err := controller.shipmentLineItemService.GetShipmentLineItemsByShipmentID(shipment.ID)
	if err != nil {
		return nil, err
	}

	//shipmentTransactions, err := controller.shipmentTransactionService.GetShipmentTransactionsByShipmentID(shipment.ID)
	//if err != nil {
	//	return nil, err
	//}

	response := responses.NewShipmentFromModel(shipment)

	response.Address = *responses.NewAddressFromModel(address)

	for _, lineItem := range shipmentLineItems {
		response.LineItems = append(response.LineItems, *responses.NewShipmentLineItemFromModel(lineItem))
	}

	//response.Transactions = *responses.NewTransactionListFromModelsList(shipmentTransactions)

	return response, nil
}
