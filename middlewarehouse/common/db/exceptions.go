package db

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

// Generic database exception
type DatabaseException struct {
	cls string `json:"type"`
	exceptions.Exception
}

func NewDatabaseException(error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return DatabaseException{
		cls:       "database",
		Exception: exceptions.Exception{error},
	}
}
