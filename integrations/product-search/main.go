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
	if req == nil {
		return "", &service.ServiceError{500, errors.New("HTTP request is malformed")}
	}

	if tokens, _ := req.Header["Jwt"]; len(tokens) == 1 {
		return tokens[0], nil
	}

	cookie, err := req.Cookie("Jwt")
	if err != nil {
		return "", &service.ServiceError{400, errors.New("Unable to read JWT")}
	}

	return cookie.Value, nil
}

func main() {
	elasticURL := os.Getenv("ELASTIC_URL")
	if elasticURL == "" {
		log.Fatalf("ELASTIC_URL must be set")
	}

	phoenixURL := os.Getenv("PHOENIX_URL")
	if phoenixURL == "" {
		log.Fatalf("PHOENIX_URL must be set")
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

		client := service.NewClient(elasticURL, phoenixURL, jwt)
		productID, svcErr := client.FindProductID(context, payload.Field, payload.Value)
		if svcErr != nil {
			svcErr.Response(c)
			return
		}

		var statusCode int
		var respBody map[string]interface{}

		if productID == 0 {
			statusCode, respBody, svcErr = client.CreateProduct(context, payload.Product)
		} else {
			statusCode, respBody, svcErr = client.UpdateProduct(context, productID, payload.Product)
		}

		if svcErr != nil {
			svcErr.Response(c)
			return
		}

		c.JSON(statusCode, respBody)
	})

    port:= os.Getenv("PORT")
    r.Run(":" + port)
}
