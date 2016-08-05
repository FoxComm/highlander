package mocks

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type AddressRepositoryMock struct {
	mock.Mock
}

func (service *AddressRepositoryMock) GetAddressByID(id uint) (*models.Address, error) {
	args := service.Called(id)

	if model, ok := args.Get(0).(*models.Address); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *AddressRepositoryMock) CreateAddress(address *models.Address) (*models.Address, error) {
	args := service.Called(address)

	if model, ok := args.Get(0).(*models.Address); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *AddressRepositoryMock) DeleteAddress(id uint) error {
	args := service.Called(id)

	if args.Bool(0) {
		return nil
	}

	return args.Error(1)
}