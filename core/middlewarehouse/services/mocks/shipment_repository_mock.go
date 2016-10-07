package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type ShipmentRepositoryMock struct {
	mock.Mock
}

func (service *ShipmentRepositoryMock) GetShipmentsByOrder(referenceNumber string) ([]*models.Shipment, error) {
	args := service.Called(referenceNumber)

	if model, ok := args.Get(0).([]*models.Shipment); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentRepositoryMock) GetShipmentByID(id uint) (*models.Shipment, error) {
	args := service.Called(id)

	if model, ok := args.Get(0).(*models.Shipment); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentRepositoryMock) CreateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	args := service.Called(shipment)

	if model, ok := args.Get(0).(*models.Shipment); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentRepositoryMock) UpdateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	args := service.Called(shipment)

	if model, ok := args.Get(0).(*models.Shipment); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentRepositoryMock) DeleteShipment(id uint) error {
	args := service.Called(id)

	return args.Error(0)
}
