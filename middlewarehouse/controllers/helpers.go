package controllers

import (
	"fmt"
	"log"
	"strconv"
	"strings"

	"github.com/FoxComm/highlander/middlewarehouse/common/failures"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
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

func handleServiceError(c *gin.Context, exception exceptions.IException) {
	fail := getFailure(exception)

	logFailure(fail)

	failures.Abort(c, fail)
}

func getFailure(exception exceptions.IException) failures.Failure {
	if _, ok := exception.(repositories.EntityNotFoundException); ok {
		return failures.NewNotFound(exception)
	}

	return failures.NewBadRequest(exception)
}

func logFailure(fail failures.Failure) {
	messages := []string{}
	for _, err := range fail.ToJSON().Errors {
		messages = append(messages, fmt.Sprintf("ServiceError: %s", err))
	}

	log.Println(strings.Join(messages, "\n"))
}
