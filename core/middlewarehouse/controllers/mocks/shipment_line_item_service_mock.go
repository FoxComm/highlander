package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type ShipmentLineItemServiceMock struct {
	mock.Mock
}

func (service *ShipmentLineItemServiceMock) GetShipmentLineItemsByShipmentID(id uint) ([]*models.ShipmentLineItem, error) {
	args := service.Called(id)

	if models, ok := args.Get(0).([]*models.ShipmentLineItem); ok {
		return models, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentLineItemServiceMock) GetShipmentLineItemByID(id uint) (*models.ShipmentLineItem, error) {
	args := service.Called(id)

	if model, ok := args.Get(0).(*models.ShipmentLineItem); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentLineItemServiceMock) CreateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error) {
	args := service.Called(shipmentLineItem)

	if model, ok := args.Get(0).(*models.ShipmentLineItem); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentLineItemServiceMock) UpdateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, error) {
	args := service.Called(shipmentLineItem)

	if model, ok := args.Get(0).(*models.ShipmentLineItem); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentLineItemServiceMock) DeleteShipmentLineItem(id uint) error {
	args := service.Called(id)

	return args.Error(0)
}
