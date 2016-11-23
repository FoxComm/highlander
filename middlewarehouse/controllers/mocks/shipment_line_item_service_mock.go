package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type ShipmentLineItemServiceMock struct {
	mock.Mock
}

func (service *ShipmentLineItemServiceMock) GetShipmentLineItemsByShipmentID(id uint) ([]*models.ShipmentLineItem, exceptions.IException) {
	args := service.Called(id)

	if models, ok := args.Get(0).([]*models.ShipmentLineItem); ok {
		return models, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentLineItemServiceMock) GetShipmentLineItemByID(id uint) (*models.ShipmentLineItem, exceptions.IException) {
	args := service.Called(id)

	if model, ok := args.Get(0).(*models.ShipmentLineItem); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentLineItemServiceMock) CreateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, exceptions.IException) {
	args := service.Called(shipmentLineItem)

	if model, ok := args.Get(0).(*models.ShipmentLineItem); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentLineItemServiceMock) UpdateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, exceptions.IException) {
	args := service.Called(shipmentLineItem)

	if model, ok := args.Get(0).(*models.ShipmentLineItem); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentLineItemServiceMock) DeleteShipmentLineItem(id uint) exceptions.IException {
	args := service.Called(id)

	if ex, ok := args.Get(0).(exceptions.IException); ok {
		return ex
	}

	return nil
}
