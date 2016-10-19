package controllers

import (
	"errors"
	"fmt"
	"log"
	"strconv"
	"strings"

	"github.com/FoxComm/highlander/middlewarehouse/common/failures"

	"github.com/SermoDigital/jose/jws"
	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
)

func parse(c *gin.Context, model interface{}) failures.Failure {
	err := c.BindJSON(model)
	if err == nil {
		return nil
	}

	fail := failures.NewBadRequest(err)
	failures.Abort(c, fail)
	return fail
}

func paramInt(c *gin.Context, key string) (int, failures.Failure) {
	intStr := c.Params.ByName(key)
	id, err := strconv.Atoi(intStr)
	if err != nil {
		fError := fmt.Errorf("Unable to get int param %s", key)
		fail := failures.NewBadRequest(fError)
		failures.Abort(c, fail)
		return 0, fail
	}

	return id, nil
}

func paramUint(c *gin.Context, key string) (uint, failures.Failure) {
	intStr := c.Params.ByName(key)
	id, err := strconv.Atoi(intStr)
	if err != nil {
		fError := fmt.Errorf("Unable to get uint param %s", key)
		fail := failures.NewBadRequest(fError)
		failures.Abort(c, fail)
		return 0, fail
	}

	return uint(id), nil
}

func handleServiceError(c *gin.Context, err error) {
	fail := getFailure(err)

	logFailure(fail)

	failures.Abort(c, fail)
}

func getFailure(err error) failures.Failure {
	if err == gorm.ErrRecordNotFound {
		return failures.NewNotFound(err)
	}

	return failures.NewBadRequest(err)
}

func logFailure(fail failures.Failure) {
	messages := []string{}
	for _, err := range fail.ToJSON().Errors {
		messages = append(messages, fmt.Sprintf("ServiceError: %s", err))
	}

	log.Println(strings.Join(messages, "\n"))
}

func getContextScope(context *gin.Context) (string, error) {
	rawJWT := context.Request.Header.Get("JWT")
	if rawJWT == "" {
		return "", errors.New("JWT header not found in request")
	}

	token, err := jws.ParseJWT([]byte(rawJWT))
	if err != nil {
		return "", err
	}

	scope, ok := token.Claims()["scope"].(string)
	if !ok {
		return "", errors.New("No scope found in JWT")
	}

	return scope, nil
}

func ensureScopeIsValid(jwtScope string, givenScope string) error {
	if strings.HasPrefix(givenScope, jwtScope) {
		return nil
	}

	return fmt.Errorf("Given scope %s is not in JWT scope %s", givenScope, jwtScope)
}
