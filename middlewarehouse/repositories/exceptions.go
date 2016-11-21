package repositories

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

// Generic database exception
type databaseException struct {
	cls string `json:"type"`
	exceptions.Exception
}

func NewDatabaseException(error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return databaseException{
		cls: "database",
		Exception: exceptions.Exception{error},
	}
}

// Entity was not found exception
type entityNotFoundException struct {
	cls      string `json:"type"`
	entity   string
	entityId string
	exceptions.Exception
}

func NewEntityNotFoundException(entity string, entityId string, error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return entityNotFoundException{
		cls:       "entityNotFound",
		entity:    entity,
		entityId:  entityId,
		Exception: exceptions.Exception{error},
	}
}
