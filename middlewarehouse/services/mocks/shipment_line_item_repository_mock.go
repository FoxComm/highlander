package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/stretchr/testify/mock"
)

type ShipmentLineItemRepositoryMock struct {
	mock.Mock
}

func (service *ShipmentLineItemRepositoryMock) GetShipmentLineItemsByShipmentID(id uint) ([]*models.ShipmentLineItem, exceptions.IException) {
	args := service.Called(id)

	if model, ok := args.Get(0).([]*models.ShipmentLineItem); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentLineItemRepositoryMock) CreateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, exceptions.IException) {
	args := service.Called(shipmentLineItem)

	if model, ok := args.Get(0).(*models.ShipmentLineItem); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentLineItemRepositoryMock) UpdateShipmentLineItem(shipmentLineItem *models.ShipmentLineItem) (*models.ShipmentLineItem, exceptions.IException) {
	args := service.Called(shipmentLineItem)

	if model, ok := args.Get(0).(*models.ShipmentLineItem); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentLineItemRepositoryMock) DeleteShipmentLineItem(id uint) exceptions.IException {
	args := service.Called(id)

	if ex, ok := args.Get(0).(exceptions.IException); ok {
		return ex
	}

	return nil
}
