package controllers

import (
	"fmt"
	"log"
	"strconv"
	"strings"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/common/failures"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"

	"encoding/json"
	"github.com/gin-gonic/gin"
)

func parse(context *gin.Context, model interface{}) failures.Failure {
	err := context.BindJSON(model)
	if err == nil {
		return nil
	}

	fail := failures.NewBadRequest(exceptions.NewValidationException(err))
	failures.Abort(context, fail)
	return fail
}

func paramInt(context *gin.Context, key string) (int, failures.Failure) {
	intStr := context.Params.ByName(key)
	id, err := strconv.Atoi(intStr)
	if err != nil {
		exception := exceptions.NewValidationException(fmt.Errorf("Unable to get int param %s", key))
		fail := failures.NewBadRequest(exception)
		failures.Abort(context, fail)
		return 0, fail
	}

	return id, nil
}

func paramUint(context *gin.Context, key string) (uint, failures.Failure) {
	intStr := context.Params.ByName(key)
	id, err := strconv.Atoi(intStr)
	if err != nil {
		exception := exceptions.NewValidationException(fmt.Errorf("Unable to get uint param %s", key))
		fail := failures.NewBadRequest(exception)
		failures.Abort(context, fail)
		return 0, fail
	}

	return uint(id), nil
}

func handleServiceError(context *gin.Context, exception exceptions.IException) {
	fail := getFailure(exception)

	logFailure(fail)

	failures.Abort(context, fail)
}

func getFailure(exception exceptions.IException) failures.Failure {
	if _, ok := exception.(repositories.EntityNotFoundException); ok {
		return failures.NewNotFound(exception)
	}

	return failures.NewBadRequest(exception)
}

func logFailure(failure failures.Failure) {
	messages := []string{}
	for _, err := range failure.ToJSON().Errors {
		formattedError, _ := json.MarshalIndent(err, "", "    ")
		messages = append(messages, fmt.Sprintf("ServiceError: %s", formattedError))
	}

	log.Println(strings.Join(messages, "\n"))
}
