package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type StockLocationServiceMock struct {
	mock.Mock
}

func (service *StockLocationServiceMock) GetLocations() ([]*models.StockLocation, error) {
	args := service.Called()

	if models, ok := args.Get(0).([]*models.StockLocation); ok {
		return models, nil
	}

	return nil, args.Error(1)
}

func (service *StockLocationServiceMock) GetLocationByID(id uint) (*models.StockLocation, error) {
	args := service.Called(id)

	if model, ok := args.Get(0).(*models.StockLocation); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *StockLocationServiceMock) CreateLocation(location *models.StockLocation) (*models.StockLocation, error) {
	args := service.Called(location)

	if model, ok := args.Get(0).(*models.StockLocation); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *StockLocationServiceMock) UpdateLocation(location *models.StockLocation) (*models.StockLocation, error) {
	args := service.Called(location)

	if model, ok := args.Get(0).(*models.StockLocation); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *StockLocationServiceMock) DeleteLocation(id uint) error {
	args := service.Called(id)

	return args.Error(0)
}
