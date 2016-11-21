package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/stretchr/testify/mock"
)

type ShipmentRepositoryMock struct {
	mock.Mock
}

func (service *ShipmentRepositoryMock) GetShipmentsByOrder(referenceNumber string) ([]*models.Shipment, exceptions.IException) {
	args := service.Called(referenceNumber)

	if model, ok := args.Get(0).([]*models.Shipment); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentRepositoryMock) GetShipmentByID(id uint) (*models.Shipment, exceptions.IException) {
	args := service.Called(id)

	if model, ok := args.Get(0).(*models.Shipment); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentRepositoryMock) CreateShipment(shipment *models.Shipment) (*models.Shipment, exceptions.IException) {
	args := service.Called(shipment)

	if model, ok := args.Get(0).(*models.Shipment); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentRepositoryMock) UpdateShipment(shipment *models.Shipment) (*models.Shipment, exceptions.IException) {
	args := service.Called(shipment)

	if model, ok := args.Get(0).(*models.Shipment); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShipmentRepositoryMock) DeleteShipment(id uint) exceptions.IException {
	args := service.Called(id)

	if ex, ok := args.Get(0).(exceptions.IException); ok {
		return ex
	}

	return nil
}
