package controllers

import (
	"errors"
	"fmt"
	"strconv"
	"strings"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/failures"

	"github.com/SermoDigital/jose/jwt"
	"github.com/gin-gonic/gin"
)

func parse(context *gin.Context, model interface{}) failures.Failure {
	err := context.BindJSON(model)
	if err == nil {
		return nil
	}

	fail := failures.NewBadRequest(err)
	failures.Abort(context, fail)
	return fail
}

func paramInt(context *gin.Context, key string) (int, failures.Failure) {
	intStr := context.Params.ByName(key)
	id, err := strconv.Atoi(intStr)
	if err != nil {
		fError := fmt.Errorf("Unable to get int param %s", key)
		fail := failures.NewBadRequest(fError)
		failures.Abort(context, fail)
		return 0, fail
	}

	return id, nil
}

func paramUint(context *gin.Context, key string) (uint, failures.Failure) {
	intStr := context.Params.ByName(key)
	id, err := strconv.Atoi(intStr)
	if err != nil {
		fError := fmt.Errorf("Unable to get uint param %s", key)
		fail := failures.NewBadRequest(fError)
		failures.Abort(context, fail)
		return 0, fail
	}

	return uint(id), nil
}

func setScope(context *gin.Context, scopable payloads.IScopable) bool {
	contextScope, err := getContextScope(context)
	if err != nil {
		failures.HandleServiceError(context, err)
		return false
	}

	payloadScope := scopable.GetScope()

	//use context scope if no scope in payload
	if payloadScope == "" {
		scopable.SetScope(contextScope)
		return true
	}

	//ensure payload scope is in context
	if payloadScope != contextScope && !strings.HasPrefix(payloadScope, contextScope+".") {
		failures.HandleServiceError(context, fmt.Errorf("Payload scope %s is not in JWT scope %s", payloadScope, contextScope))
		return false
	}

	return true
}

func getContextScope(context *gin.Context) (string, error) {
	token := context.Keys["jwt"].(jwt.JWT)
	scope, ok := token.Claims()["scope"].(string)
	if !ok {
		return "", errors.New("No scope found in JWT")
	}

	return scope, nil
}
