package mocks

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type ShipmentRepositoryMock struct {
	mock.Mock
}

func (service *ShipmentRepositoryMock) GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error) {
	args := service.Called(referenceNumber)

	if model, ok := args.Get(0).([]*models.Shipment); ok {
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
