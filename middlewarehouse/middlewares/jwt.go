package middlewares

import (
	"errors"
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/common/failures"
	"github.com/SermoDigital/jose/jws"
	"github.com/gin-gonic/gin"
)

func FetchJWT(context *gin.Context) {
	rawJWT := context.Request.Header.Get("JWT")

	//override, if JWT given in cookie
	if cookieJWT, err := context.Request.Cookie("JWT"); err == nil {
		rawJWT = cookieJWT.Value
	}

	if rawJWT == "" {
		failures.HandleServiceError(context, errors.New("No JWT passed"))
		return
	}

	token, err := jws.ParseJWT([]byte(rawJWT))
	if err != nil {
		failures.HandleServiceError(context, fmt.Errorf("Token parse failure: %s", err.Error()))
		return
	}

	context.Keys = map[string]interface{}{
		"jwt": token,
	}

	context.Next()
}
