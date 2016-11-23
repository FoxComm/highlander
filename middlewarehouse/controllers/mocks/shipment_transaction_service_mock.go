package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type ShipmentTransactionServiceMock struct {
	mock.Mock
}

func (service *ShipmentTransactionServiceMock) GetShipmentTransactionsByShipmentID(id uint) ([]*models.ShipmentTransaction, exceptions.IException) {
	args := service.Called(id)

	if models, ok := args.Get(0).([]*models.ShipmentTransaction); ok {
		return models, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentTransactionServiceMock) GetShipmentTransactionByID(id uint) (*models.ShipmentTransaction, exceptions.IException) {
	args := service.Called(id)

	if model, ok := args.Get(0).(*models.ShipmentTransaction); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentTransactionServiceMock) CreateShipmentTransaction(shipmentTransaction *models.ShipmentTransaction) (*models.ShipmentTransaction, exceptions.IException) {
	args := service.Called(shipmentTransaction)

	if model, ok := args.Get(0).(*models.ShipmentTransaction); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentTransactionServiceMock) UpdateShipmentTransaction(shipmentTransaction *models.ShipmentTransaction) (*models.ShipmentTransaction, exceptions.IException) {
	args := service.Called(shipmentTransaction)

	if model, ok := args.Get(0).(*models.ShipmentTransaction); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentTransactionServiceMock) DeleteShipmentTransaction(id uint) exceptions.IException {
	args := service.Called(id)

	if ex, ok := args.Get(0).(exceptions.IException); ok {
		return ex
	}

	return nil
}
