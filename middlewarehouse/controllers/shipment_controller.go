package controllers

import (
	"errors"
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/services"

	"github.com/gin-gonic/gin"
)

type shipmentController struct {
	shipmentService services.IShipmentService
}

func NewShipmentController(
	shipmentService services.IShipmentService,
) IController {
	return &shipmentController{shipmentService}
}

func (controller *shipmentController) SetUp(router gin.IRouter) {
	router.GET(":orderRef", controller.getShipmentsByOrder())
	router.POST("", controller.createShipment())
	router.PATCH("for-order/:orderRef", controller.updateShipmentForOrder())
	router.POST("from-order", controller.createShipmentFromOrder())
}

func (controller *shipmentController) getShipmentsByOrder() gin.HandlerFunc {
	return func(context *gin.Context) {
		referenceNumber := context.Params.ByName("orderRef")
		shipments, err := controller.shipmentService.GetShipmentsByOrder(referenceNumber)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		response := &responses.Shipments{}
		for _, shipment := range shipments {
			resp, err := responses.NewShipmentFromModel(shipment)
			if err != nil {
				handleServiceError(context, err)
				return
			}

			response.Shipments = append(response.Shipments, *resp)
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

		response, err := responses.NewShipmentFromModel(shipment)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusCreated, response)
	}
}

func (controller *shipmentController) updateShipmentForOrder() gin.HandlerFunc {
	return func(context *gin.Context) {
		payload := &payloads.UpdateShipment{}
		if parse(context, payload) != nil {
			return
		}

		orderRef := context.Params.ByName("orderRef")
		if orderRef == "" {
			err := errors.New("Order Reference not specified")
			handleServiceError(context, err)
			return
		}

		model := models.NewShipmentFromUpdatePayload(payload)
		model.OrderRefNum = orderRef

		shipment, err := controller.shipmentService.UpdateShipmentForOrder(model)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		response, err := responses.NewShipmentFromModel(shipment)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusOK, response)
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

		//If the shipment has no line items with tracked inventory, then it can be automatically shipped.
		//Most useful in the case of gift cards.
		hasTrackedInventory := false
		for _, lineItem := range payload.LineItems.SKUs {
			// We only care about the line items if we're tracking inventory.
			if lineItem.TrackInventory {
				hasTrackedInventory = true
				break
			}
		}

		//This means that it's only digital items (eg. gift cards)
		if !hasTrackedInventory {
			shipment.State = models.ShipmentStateShipped
			shipment, err = controller.shipmentService.UpdateShipment(shipment)
			if err != nil {
				handleServiceError(context, err)
				return
			}
		}

		response, err := responses.NewShipmentFromModel(shipment)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusCreated, response)
	}
}
