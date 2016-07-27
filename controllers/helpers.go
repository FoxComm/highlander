package controllers

import (
	"fmt"
	"strconv"

	"github.com/FoxComm/middlewarehouse/common/failures"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
)

func parse(c *gin.Context, model interface{}) failures.Failure {
	err := c.BindJSON(model)
	if err == nil {
		return nil
	}

<<<<<<< 127850d5e7abfb83c9177a36dc54db08f82e3101
	err := errors.New("Invalid payload")
	fail := failures.NewBadRequest(err)
=======
	fail := failures.MakeBadRequest(err)
>>>>>>> Improved controllers/helpers parse helper to fail with exact binding error
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

func handleServiceError(c *gin.Context, err error) failures.Failure {
	var fail failures.Failure
	if err == gorm.ErrRecordNotFound {
		fail = failures.NewNotFound(err)
	} else {
		fail = failures.NewBadRequest(err)
	}
	failures.Abort(c, fail)

	return fail
}
