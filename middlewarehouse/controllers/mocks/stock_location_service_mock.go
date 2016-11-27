package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type StockLocationServiceMock struct {
	mock.Mock
}

func (service *StockLocationServiceMock) GetLocations() ([]*models.StockLocation, exceptions.IException) {
	args := service.Called()

	if models, ok := args.Get(0).([]*models.StockLocation); ok {
		return models, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *StockLocationServiceMock) GetLocationByID(id uint) (*models.StockLocation, exceptions.IException) {
	args := service.Called(id)

	if model, ok := args.Get(0).(*models.StockLocation); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *StockLocationServiceMock) CreateLocation(location *models.StockLocation) (*models.StockLocation, exceptions.IException) {
	args := service.Called(location)

	if model, ok := args.Get(0).(*models.StockLocation); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *StockLocationServiceMock) UpdateLocation(location *models.StockLocation) (*models.StockLocation, exceptions.IException) {
	args := service.Called(location)

	if model, ok := args.Get(0).(*models.StockLocation); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *StockLocationServiceMock) DeleteLocation(id uint) exceptions.IException {
	args := service.Called(id)

	if ex, ok := args.Get(0).(exceptions.IException); ok {
		return ex
	}

	return nil
}
