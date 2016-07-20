package routes

import (
	"net/http"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/services"
	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
)

// /carriers router group handler
func runCarriers(router gin.IRouter) {
	router.GET("/", func(context *gin.Context) {
		carriers, err := services.GetCarriers()
		//ensure fetched successfully
		if err != nil {
			context.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		//convert to responses slice
		response := make([]*responses.Carrier, len(carriers))
		for i := range carriers {
			response[i] = responses.NewCarrierFromModel(carriers[i])
		}
		context.JSON(http.StatusOK, response)
	})

	router.GET("/:id", func(context *gin.Context) {
		//get id from context
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		//get carrier by id
		carrier, err := getCarrierByID(context, id)
		if err != nil {
			return
		}

		context.JSON(http.StatusOK, responses.NewCarrierFromModel(carrier))
	})

	router.POST("/", func(context *gin.Context) {
		//try parse payload
		var payload payloads.Carrier
		if parse(context, &payload) != nil {
			return
		}

		//try create
		if id, err := services.CreateCarrier(&payload); err == nil {
			context.JSON(http.StatusCreated, id)
		} else {
			context.AbortWithError(http.StatusBadRequest, err)
		}
	})

	router.PUT("/:id", func(context *gin.Context) {
		//try parse payload
		var payload payloads.Carrier
		if parse(context, &payload) != nil {
			return
		}

		//get id from context
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		//check carrier existence
		if _, err := getCarrierByID(context, id); err != nil {
			return
		}

		if err := services.UpdateCarrier(id, &payload); err != nil {
			context.AbortWithError(http.StatusBadRequest, err)
			return
		}

		context.Writer.WriteHeader(http.StatusNoContent)
	})

	router.DELETE("/:id", func(context *gin.Context) {
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		//get carrier by id
		if _, err := getCarrierByID(context, id); err != nil {
			return
		}

		if err := services.DeleteCarrier(uint(id)); err != nil {
			context.AbortWithError(http.StatusBadRequest, err)
			return
		}

		context.Writer.WriteHeader(http.StatusNoContent)
	})
}

func getCarrierByID(context *gin.Context, id uint) (*models.Carrier, error) {
	carrier, err := services.GetCarrierById(id)

	switch err {
	case nil:
		return carrier, nil
	case gorm.ErrRecordNotFound:
		context.AbortWithStatus(http.StatusNotFound)
	default:
		context.AbortWithError(http.StatusBadRequest, err)
	}

	return nil, err
}
