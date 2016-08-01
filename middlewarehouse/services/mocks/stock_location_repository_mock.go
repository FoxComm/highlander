package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type StockLocationRepositoryMock struct {
	mock.Mock
}

func (repository *StockLocationRepositoryMock) GetLocations() ([]*models.StockLocation, error) {
	args := repository.Called()

	if models, ok := args.Get(0).([]*models.StockLocation); ok {
		return models, nil
	}

	return nil, args.Error(1)
}

func (repository *StockLocationRepositoryMock) GetLocationByID(id uint) (*models.StockLocation, error) {
	args := repository.Called(id)

	if model, ok := args.Get(0).(*models.StockLocation); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (repository *StockLocationRepositoryMock) CreateLocation(location *models.StockLocation) (*models.StockLocation, error) {
	args := repository.Called(location)

	if model, ok := args.Get(0).(*models.StockLocation); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (repository *StockLocationRepositoryMock) UpdateLocation(location *models.StockLocation) (*models.StockLocation, error) {
	args := repository.Called(location)

	if model, ok := args.Get(0).(*models.StockLocation); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (repository *StockLocationRepositoryMock) DeleteLocation(id uint) error {
	args := repository.Called(id)

	return args.Error(0)
}
