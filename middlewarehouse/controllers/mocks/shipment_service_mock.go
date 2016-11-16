package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type ShipmentServiceMock struct {
	mock.Mock
}

func (service *ShipmentServiceMock) GetShipmentsByOrder(referenceNumber string) ([]*models.Shipment, error) {
	args := service.Called(referenceNumber)

	if models, ok := args.Get(0).([]*models.Shipment); ok {
		return models, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentServiceMock) CreateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	args := service.Called(shipment)

	if model, ok := args.Get(0).(*models.Shipment); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentServiceMock) UpdateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	args := service.Called(shipment)

	if model, ok := args.Get(0).(*models.Shipment); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentServiceMock) UpdateShipmentForOrder(shipment *models.Shipment) (*models.Shipment, error) {
	args := service.Called(shipment)

	if model, ok := args.Get(0).(*models.Shipment); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentServiceMock) GetUnshippedItems(shipment *models.Shipment) ([]*models.ShipmentLineItem, error) {
	args := service.Called(shipment)

	if model, ok := args.Get(0).([]*models.ShipmentLineItem); ok {
		return model, nil
	}

	return nil, args.Error(1)
}
