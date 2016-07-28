package mocks

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type CarrierRepositoryMock struct {
	mock.Mock
}

func (service *CarrierRepositoryMock) GetCarriers() ([]*models.Carrier, error) {
	args := service.Called()

	if models, ok := args.Get(0).([]*models.Carrier); ok {
		return models, nil
	}

	return nil, args.Error(1)
}

func (service *CarrierRepositoryMock) GetCarrierByID(id uint) (*models.Carrier, error) {
	args := service.Called(id)

	if model, ok := args.Get(0).(*models.Carrier); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *CarrierRepositoryMock) CreateCarrier(carrier *models.Carrier) (*models.Carrier, error) {
	args := service.Called(carrier)

	if model, ok := args.Get(0).(*models.Carrier); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *CarrierRepositoryMock) UpdateCarrier(carrier *models.Carrier) (*models.Carrier, error) {
	args := service.Called(carrier)

	if model, ok := args.Get(0).(*models.Carrier); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *CarrierRepositoryMock) DeleteCarrier(id uint) error {
	args := service.Called(id)

	if args.Bool(0) {
		return nil
	}

	return args.Error(1)
}
