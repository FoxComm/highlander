package main

import (
	"errors"
	"log"
	"net/http"
	"os"

	"github.com/FoxComm/highlander/integrations/product-search/service"
	"github.com/gin-gonic/gin"
)

func getJWT(req *http.Request) (string, *service.ServiceError) {
	if tokens, _ := req.Header["Jwt"]; len(tokens) == 1 {
		return tokens[0], nil
	}

	return "", &service.ServiceError{400, errors.New("JWT not found in request header")}
}

func main() {
	apiURL := os.Getenv("API_URL")
	if apiURL == "" {
		log.Fatalf("API_URL must be set")
	}

	r := gin.Default()
	r.POST("/products/by-field/:context", func(c *gin.Context) {
		context := c.Param("context")

		payload := new(UpdatePayload)
		if err := c.BindJSON(payload); err != nil {
			se := service.ServiceError{400, err}
			se.Response(c)
			return
		}

		jwt, err := getJWT(c.Request)
		if err != nil {
			err.Response(c)
			return
		}

		client := service.NewClient(apiURL, jwt)
		productID, svcErr := client.FindProductID(context, payload.Field, payload.Value)
		if svcErr != nil {
			svcErr.Response(c)
			return
		}

		statusCode, respBody, svcErr := client.UpdateProduct(context, productID, payload.Product)
		if svcErr != nil {
			svcErr.Response(c)
			return
		}

		c.JSON(statusCode, respBody)
	})

	r.Run(":5492")
}
