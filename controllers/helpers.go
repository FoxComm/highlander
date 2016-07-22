package controllers

import (
	"errors"
	"fmt"
	"strconv"

	"github.com/gin-gonic/gin"

	"github.com/FoxComm/middlewarehouse/common/failures"
	"github.com/jinzhu/gorm"
)

func parse(c *gin.Context, model interface{}) failures.Failure {
	if c.BindJSON(model) == nil {
		return nil
	}

	err := errors.New("Invalid payload")
	fail := failures.MakeBadRequest(err)
	failures.Abort(c, fail)
	return fail
}

func paramInt(c *gin.Context, key string) (int, failures.Failure) {
	intStr := c.Params.ByName(key)
	id, err := strconv.Atoi(intStr)
	if err != nil {
		fError := fmt.Errorf("Unable to get int param %s", key)
		fail := failures.MakeBadRequest(fError)
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
		fail := failures.MakeBadRequest(fError)
		failures.Abort(c, fail)
		return 0, fail
	}

	return uint(id), nil
}

func handleServiceError(c *gin.Context, err error) failures.Failure {
	var fail failures.Failure
	if err == gorm.ErrRecordNotFound {
		fail = failures.MakeNotFound(err)
	} else {
		fail = failures.MakeBadRequest(err)
	}
	failures.Abort(c, fail)

	return fail
}
