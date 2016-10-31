package controllers

import (
	"errors"
	"fmt"

	"github.com/SermoDigital/jose/jws"
	"github.com/gin-gonic/gin"
)

func FetchJWT(context *gin.Context) {
	rawJWT := context.Request.Header.Get("JWT")
	if rawJWT == "" {
		handleServiceError(context, errors.New("No JWT header passed"))
		return
	}

	token, err := jws.ParseJWT([]byte(rawJWT))
	if err != nil {
		handleServiceError(context, fmt.Errorf("Token parse failure: %s", err.Error()))
		return
	}

	context.Keys = map[string]interface{}{
		"jwt": token,
	}

	context.Next()
}
