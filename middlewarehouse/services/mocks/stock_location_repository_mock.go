package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/stretchr/testify/mock"
)

type StockLocationRepositoryMock struct {
	mock.Mock
}

func (repository *StockLocationRepositoryMock) GetLocations() ([]*models.StockLocation, exceptions.IException) {
	args := repository.Called()

	if models, ok := args.Get(0).([]*models.StockLocation); ok {
		return models, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (repository *StockLocationRepositoryMock) GetLocationByID(id uint) (*models.StockLocation, exceptions.IException) {
	args := repository.Called(id)

	if model, ok := args.Get(0).(*models.StockLocation); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (repository *StockLocationRepositoryMock) CreateLocation(location *models.StockLocation) (*models.StockLocation, exceptions.IException) {
	args := repository.Called(location)

	if model, ok := args.Get(0).(*models.StockLocation); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (repository *StockLocationRepositoryMock) UpdateLocation(location *models.StockLocation) (*models.StockLocation, exceptions.IException) {
	args := repository.Called(location)

	if model, ok := args.Get(0).(*models.StockLocation); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (repository *StockLocationRepositoryMock) DeleteLocation(id uint) exceptions.IException {
	args := repository.Called(id)

	if ex, ok := args.Get(0).(exceptions.IException); ok {
		return ex
	}

	return nil
}
