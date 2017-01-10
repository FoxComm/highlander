package controllers

import (
	"errors"
	"net/http"
	"strings"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/services"

	"github.com/FoxComm/highlander/middlewarehouse/common/failures"
	"github.com/gin-gonic/gin"
)

const (
	outOfStockPrefix = "Not enough units"
	outOfStockError  = "Oops! Looks like one of your items is out of stock. Please remove it from the cart to complete checkout"
)

type reservationController struct {
	service services.InventoryService
}

func NewReservationController(service services.InventoryService) IController {
	return &reservationController{service}
}

func (controller *reservationController) SetUp(router gin.IRouter) {
	router.Use(FetchJWT)
	router.POST("hold", controller.Hold())
	router.DELETE("hold/:refNum", controller.Unhold())
}

func (controller *reservationController) Hold() gin.HandlerFunc {
	return func(context *gin.Context) {
		payload := &payloads.Reservation{}
		if parse(context, payload) != nil {
			return
		}

		if !setScope(context, payload) {
			return
		}

		if err := payload.Validate(); err != nil {
			fail := failures.NewBadRequest(err)
			failures.Abort(context, fail)
			return
		}

		skuMap := map[string]int{}
		for _, sku := range payload.Items {
			skuMap[sku.SKU] = int(sku.Qty)
		}

		if err := controller.service.HoldItems(payload.RefNum, skuMap); err != nil {
			// This is ugly - let's spike into error messages in MWH in the future.
			if strings.HasPrefix(err.Error(), outOfStockPrefix) {
				handleServiceError(context, errors.New(outOfStockError))
			} else {
				handleServiceError(context, err)
			}
			return
		}

		context.Status(http.StatusNoContent)
	}
}

func (controller *reservationController) Unhold() gin.HandlerFunc {
	return func(context *gin.Context) {
		refNum := context.Params.ByName("refNum")

		if err := controller.service.ReleaseItems(refNum); err != nil {
			handleServiceError(context, err)
			return
		}

		context.Status(http.StatusNoContent)
	}
}
