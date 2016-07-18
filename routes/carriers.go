package routes

import (
	"net/http"
	"strconv"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/services"
	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
)

// /carriers router group handler
func runCarriers(router gin.IRouter) {
	router.GET("/", func(context *gin.Context) {
		carriers, err := services.GetCarriers()
		if err != nil {
			context.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		response := make([]*responses.Carrier, len(carriers))
		for i := range carriers {
			response[i] = responses.NewCarrierFromModel(carriers[i])
		}
		context.JSON(http.StatusOK, response)
	})

	router.GET("/:id", func(context *gin.Context) {
		idStr := context.Params.ByName("id")

		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			context.AbortWithError(http.StatusBadRequest, err)
			return
		}

		carrier, err := services.GetCarrierById(uint(id))
		if err != nil {
			if err == gorm.ErrRecordNotFound {
				context.AbortWithStatus(http.StatusNotFound)
			} else {
				context.AbortWithError(http.StatusInternalServerError, err)
			}
			return
		}

		context.JSON(http.StatusOK, responses.NewCarrierFromModel(carrier))
	})

	router.POST("/", func(context *gin.Context) {
		var payload *payloads.Carrier
		if parse(context, payload) != nil {
			return
		}

		if err := services.CreateCarrier(payload); err != nil {
			context.AbortWithError(http.StatusBadRequest, err)
			return
		}

		context.JSON(http.StatusCreated, nil)
	})

	router.PUT("/:id", func(context *gin.Context) {
		idStr := context.Params.ByName("id")
		var payload *payloads.Carrier
		if parse(context, payload) != nil {
			return
		}

		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			context.AbortWithError(http.StatusBadRequest, err)
			return
		}

		if err = services.UpdateCarrier(uint(id), payload); err != nil {
			context.AbortWithError(http.StatusBadRequest, err)
			return
		}

		context.JSON(http.StatusOK, nil)
	})

	router.DELETE("/:id", func(context *gin.Context) {
		idStr := context.Params.ByName("id")

		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			context.AbortWithError(http.StatusBadRequest, err)
			return
		}

		if err = services.DeleteCarrier(uint(id)); err != nil {
			context.AbortWithError(http.StatusBadRequest, err)
			return
		}

		context.JSON(http.StatusOK, nil)
	})
}
