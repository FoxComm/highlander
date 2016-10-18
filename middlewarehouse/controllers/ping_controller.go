package controllers

import (
	"fmt"
	"net/http"

	"github.com/SermoDigital/jose/jws"
	"github.com/gin-gonic/gin"
)

type pingController struct {
}

func NewPingController() IController {
	return &pingController{}
}

func (controller *pingController) SetUp(router gin.IRouter) {
	router.GET("", controller.HealthCheck())
	router.GET("jwt", controller.Jwt())
}

func (controller *pingController) HealthCheck() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Status(http.StatusNoContent)
	}
}

func (controller *pingController) Jwt() gin.HandlerFunc {
	return func(context *gin.Context) {
		scope, err := getContextScope(context)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		fmt.Println(scope)
		context.Status(http.StatusNoContent)
	}
}

func getContextScope(context *gin.Context) (string, error) {
	rawJWT := context.Request.Header.Get("JWT")

	token, err := jws.ParseJWT([]byte(rawJWT))

	if err != nil {
		return "", err
	}

    if scope, ok := token.Claims()["scope"].(string); ok {
        return scope, nil
    }

	return "JWT parsed", nil
}
