package repositories

import ("github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

// Generic database exception
type databaseException struct {
	error    error
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
		error:    error,
	}
}

// Entity was not found exception
type entityNotFoundException struct {
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

func NewEntityNotFound(entity string, entityId string, error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return entityNotFoundException{
		entity:   entity,
		entityId: entityId,
		error:    error,
	}
}
