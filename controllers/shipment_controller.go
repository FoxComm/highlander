package controllers

import (
	"net/http"
	"strings"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/services"

	"github.com/gin-gonic/gin"
)

type shipmentController struct {
	shipmentService         services.IShipmentService
	shipmentLineItemService services.IShipmentLineItemService
	//shipmentTransactionService services.IShipmentTransactionService
}

func NewShipmentController(
	shipmentService services.IShipmentService,
	shipmentLineItemService services.IShipmentLineItemService,
	//shipmentTransactionService services.IShipmentTransactionService,
) IController {
	return &shipmentController{shipmentService, shipmentLineItemService /*, shipmentTransactionService*/}
}

func (controller *shipmentController) SetUp(router gin.IRouter) {
	router.GET(":referenceNumbers", controller.getShipmentsByReferenceNumbers())
	router.POST("", controller.createShipment())
	router.PUT(":id", controller.updateShipment())
}

func (controller *shipmentController) getShipmentsByReferenceNumbers() gin.HandlerFunc {
	return func(context *gin.Context) {
		referenceNumbers := strings.Split(context.Params.ByName("referenceNumbers"), ",")

		response := []*responses.Shipment{}
		for _, referenceNumber := range referenceNumbers {
			shipments, err := controller.shipmentService.GetShipmentsByReferenceNumber(referenceNumber)
			if err != nil {
				handleServiceError(context, err)
				return
			}

			for _, shipment := range shipments {
				response = append(response, responses.NewShipmentFromModel(shipment))
			}
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

		shipment, err := controller.shipmentService.CreateShipment(models.NewShipmentFromPayload(payload))
		if err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusCreated, responses.NewShipmentFromModel(shipment))
	}
}

func (controller *shipmentController) updateShipment() gin.HandlerFunc {
	return func(context *gin.Context) {
		payload := &payloads.Shipment{}
		if parse(context, payload) != nil {
			return
		}

		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		model := models.NewShipmentFromPayload(payload)
		model.ID = id
		for i, _ := range model.ShipmentLineItems {
			model.ShipmentLineItems[i].ShipmentID = model.ID
		}
		shipment, err := controller.shipmentService.UpdateShipment(model)

		if err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusOK, responses.NewShipmentFromModel(shipment))
	}
}
