package repositories

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

// Generic database exception
type databaseException struct {
	cls      string `json:"type"`
	error error
}

func (exception databaseException) ToString() string {
	return exception.error.Error()
}

func (exception databaseException) ToJSON() interface{} {
	return exception
}

func NewDatabaseException(error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return databaseException{
		cls:   "database",
		error: error,
	}
}

// Entity was not found exception
type entityNotFoundException struct {
	cls      string `json:"type"`
	entity   string
	entityId string
	error    error
}

func (exception entityNotFoundException) ToString() string {
	return exception.error.Error()
}

func (exception entityNotFoundException) ToJSON() interface{} {
	return exception
}

func NewEntityNotFoundException(entity string, entityId string, error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return entityNotFoundException{
		cls:      "entityNotFound",
		entity:   entity,
		entityId: entityId,
		error:    error,
	}
}
