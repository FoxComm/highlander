package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/stretchr/testify/mock"
)

type CarrierRepositoryMock struct {
	mock.Mock
}

func (service *CarrierRepositoryMock) GetCarriers() ([]*models.Carrier, exceptions.IException) {
	args := service.Called()

	if models, ok := args.Get(0).([]*models.Carrier); ok {
		return models, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}
	return nil, nil
}

func (service *CarrierRepositoryMock) GetCarrierByID(id uint) (*models.Carrier, exceptions.IException) {
	args := service.Called(id)

	if model, ok := args.Get(0).(*models.Carrier); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *CarrierRepositoryMock) CreateCarrier(carrier *models.Carrier) (*models.Carrier, exceptions.IException) {
	args := service.Called(carrier)

	if model, ok := args.Get(0).(*models.Carrier); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *CarrierRepositoryMock) UpdateCarrier(carrier *models.Carrier) (*models.Carrier, exceptions.IException) {
	args := service.Called(carrier)

	if model, ok := args.Get(0).(*models.Carrier); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *CarrierRepositoryMock) DeleteCarrier(id uint) exceptions.IException {
	args := service.Called(id)

	if ex, ok := args.Get(0).(exceptions.IException); ok {
		return ex
	}

	return nil
}
