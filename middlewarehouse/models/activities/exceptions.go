package activities

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

type activityException struct {
	cls string `json:"type"`
	exceptions.Exception
}

func NewActivityException(error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return activityException{
		cls:       "activity",
		Exception: exceptions.Exception{error},
	}
}
