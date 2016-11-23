package db

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

// Generic database exception
type DatabaseException struct {
	Type string `json:"type"`
	exceptions.Exception
}

func (exception DatabaseException) ToJSON() interface{} {
	return exception
}

func NewDatabaseException(error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return DatabaseException{
		Type:      "database",
		Exception: exceptions.Exception{error.Error()},
	}
}
