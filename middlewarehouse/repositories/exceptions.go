package repositories

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

// Entity was not found exception
type EntityNotFoundException struct {
	Type     string `json:"type"`
	Entity   string `json:"entity"`
	EntityID string `json:"entityId"`
	exceptions.Exception
}

func (exception EntityNotFoundException) ToJSON() interface{} {
	return exception
}

func NewEntityNotFoundException(entity string, entityId string, error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return EntityNotFoundException{
		Type:      "entityNotFound",
		Entity:    entity,
		EntityID:  entityId,
		Exception: exceptions.Exception{error.Error()},
	}
}
