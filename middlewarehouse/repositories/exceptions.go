package repositories

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

// Entity was not found exception
type EntityNotFoundException struct {
	cls      string `json:"type"`
	entity   string
	entityId string
	exceptions.Exception
}

func NewEntityNotFoundException(entity string, entityId string, error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return EntityNotFoundException{
		cls:       "entityNotFound",
		entity:    entity,
		entityId:  entityId,
		Exception: exceptions.Exception{error},
	}
}
