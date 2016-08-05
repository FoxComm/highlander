package mocks

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type ShipmentLineItemRepositoryMock struct {
	mock.Mock
}

func (service *ShipmentLineItemRepositoryMock) GetShipmentLineItemsByShipmentID(id uint) ([]*models.ShipmentLineItem, error) {
	args := service.Called(id)

	if model, ok := args.Get(0).([]*models.ShipmentLineItem); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentLineItemRepositoryMock) CreateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error) {
	args := service.Called(shipmentLineItem)

	if model, ok := args.Get(0).(*models.ShipmentLineItem); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentLineItemRepositoryMock) UpdateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error) {
	args := service.Called(shipmentLineItem)

	if model, ok := args.Get(0).(*models.ShipmentLineItem); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentLineItemRepositoryMock) DeleteShipmentLineItem(id uint) error {
	args := service.Called(id)

	if args.Bool(0) {
		return nil
	}

	return args.Error(1)
}