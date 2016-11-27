package activities

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

type activityException struct {
	Type string `json:"type"`
	exceptions.Exception
}

func (exception activityException) ToJSON() interface{} {
	return exception
}

func NewActivityException(error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return activityException{
		Type:      "activity",
		Exception: exceptions.Exception{error.Error()},
	}
}
