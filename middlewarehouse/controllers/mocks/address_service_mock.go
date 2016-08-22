package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type AddressServiceMock struct {
	mock.Mock
}

func (service *AddressServiceMock) GetAddressByID(id uint) (*models.Address, error) {
	args := service.Called(id)

	if model, ok := args.Get(0).(*models.Address); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *AddressServiceMock) CreateAddress(address *models.Address) (*models.Address, error) {
	args := service.Called(address)

	if model, ok := args.Get(0).(*models.Address); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *AddressServiceMock) UpdateAddress(address *models.Address) (*models.Address, error) {
	args := service.Called(address)

	if model, ok := args.Get(0).(*models.Address); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *AddressServiceMock) DeleteAddress(id uint) error {
	args := service.Called(id)

	return args.Error(0)
}
