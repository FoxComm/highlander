package mocks

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type ShipmentTransactionServiceMock struct {
	mock.Mock
}

func (service *ShipmentTransactionServiceMock) GetShipmentTransactionsByShipmentID(id uint) ([]*models.ShipmentTransaction, error) {
	args := service.Called(id)

	if models, ok := args.Get(0).([]*models.ShipmentTransaction); ok {
		return models, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentTransactionServiceMock) GetShipmentTransactionByID(id uint) (*models.ShipmentTransaction, error) {
	args := service.Called(id)

	if model, ok := args.Get(0).(*models.ShipmentTransaction); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentTransactionServiceMock) CreateShipmentTransaction(shipmentTransaction *models.ShipmentTransaction) (*models.ShipmentTransaction, error) {
	args := service.Called(shipmentTransaction)

	if model, ok := args.Get(0).(*models.ShipmentTransaction); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentTransactionServiceMock) UpdateShipmentTransaction(shipmentTransaction *models.ShipmentTransaction) (*models.ShipmentTransaction, error) {
	args := service.Called(shipmentTransaction)

	if model, ok := args.Get(0).(*models.ShipmentTransaction); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentTransactionServiceMock) DeleteShipmentTransaction(id uint) error {
	args := service.Called(id)

	if args.Bool(0) {
		return nil
	}

	return args.Error(1)
}
