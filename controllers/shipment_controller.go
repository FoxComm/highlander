package controllers

import (
	"log"
	"net/http"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/models/activities"
	"github.com/FoxComm/middlewarehouse/services"

	"github.com/gin-gonic/gin"
)

type shipmentController struct {
	shipmentService services.IShipmentService
	activityLogger  services.IActivityLogger
}

func NewShipmentController(
	shipmentService services.IShipmentService,
	activityLogger services.IActivityLogger,
) IController {
	return &shipmentController{shipmentService, activityLogger}
}

func (controller *shipmentController) SetUp(router gin.IRouter) {
	router.GET(":referenceNumber", controller.getShipmentsByReferenceNumber())
	router.POST("", controller.createShipment())
	router.PUT(":id", controller.updateShipment())
	router.POST("from-order", controller.createShipmentFromOrder())
}

func (controller *shipmentController) getShipmentsByReferenceNumber() gin.HandlerFunc {
	return func(context *gin.Context) {
		referenceNumber := context.Params.ByName("referenceNumber")
		shipments, err := controller.shipmentService.GetShipmentsByReferenceNumber(referenceNumber)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		response := &responses.Shipments{}
		for _, shipment := range shipments {
			response.Shipments = append(response.Shipments, *responses.NewShipmentFromModel(shipment))
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

		resp := responses.NewShipmentFromModel(shipment)
		context.JSON(http.StatusCreated, resp)

		// Having this in the controller seems wrong...
		activity, err := activities.NewShipmentCreated(resp, shipment.CreatedAt)
		if err != nil {
			// Don't respond to user with this error.
			log.Printf("Unable to create shipment created activity with error %s", err.Error())
		}

		err = controller.activityLogger.Log(activity)
		if err != nil {
			// Don't respond to user with this error.
			log.Printf("Unable to create shipment activity in Kafka with error %s", err.Error())
		}
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

func (controller *shipmentController) createShipmentFromOrder() gin.HandlerFunc {
	return func(context *gin.Context) {
		payload := &payloads.Order{}
		if parse(context, payload) != nil {
			return
		}

		shipment, err := controller.shipmentService.CreateShipment(models.NewShipmentFromOrderPayload(payload))
		if err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusCreated, responses.NewShipmentFromModel(shipment))
	}
}
