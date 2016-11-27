package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type ShipmentServiceMock struct {
	mock.Mock
}

func (service *ShipmentServiceMock) GetShipmentsByOrder(referenceNumber string) ([]*models.Shipment, exceptions.IException) {
	args := service.Called(referenceNumber)

	if models, ok := args.Get(0).([]*models.Shipment); ok {
		return models, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentServiceMock) CreateShipment(shipment *models.Shipment) (*models.Shipment, exceptions.IException) {
	args := service.Called(shipment)

	if model, ok := args.Get(0).(*models.Shipment); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentServiceMock) UpdateShipment(shipment *models.Shipment) (*models.Shipment, exceptions.IException) {
	args := service.Called(shipment)

	if model, ok := args.Get(0).(*models.Shipment); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentServiceMock) UpdateShipmentForOrder(shipment *models.Shipment) (*models.Shipment, exceptions.IException) {
	args := service.Called(shipment)

	if model, ok := args.Get(0).(*models.Shipment); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentServiceMock) GetUnshippedItems(shipment *models.Shipment) ([]*models.ShipmentLineItem, exceptions.IException) {
	args := service.Called(shipment)

	if model, ok := args.Get(0).([]*models.ShipmentLineItem); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}
