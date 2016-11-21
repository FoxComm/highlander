package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/stretchr/testify/mock"
)

type AddressRepositoryMock struct {
	mock.Mock
}

func (service *AddressRepositoryMock) GetAddressByID(id uint) (*models.Address, exceptions.IException) {
	args := service.Called(id)

	if model, ok := args.Get(0).(*models.Address); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *AddressRepositoryMock) CreateAddress(address *models.Address) (*models.Address, exceptions.IException) {
	args := service.Called(address)

	if model, ok := args.Get(0).(*models.Address); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *AddressRepositoryMock) DeleteAddress(id uint) exceptions.IException {
	args := service.Called(id)

	if ex, ok := args.Get(0).(exceptions.IException); ok {
		return ex
	}

	return nil
}
