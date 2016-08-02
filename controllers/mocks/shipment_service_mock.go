package mocks

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type ShipmentServiceMock struct {
	mock.Mock
}

func (service *ShipmentServiceMock) GetShipments() ([]*models.Shipment, error) {
	args := service.Called()

	if models, ok := args.Get(0).([]*models.Shipment); ok {
		return models, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentServiceMock) GetShipmentByID(id uint) (*models.Shipment, error) {
	args := service.Called(id)

	if model, ok := args.Get(0).(*models.Shipment); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShipmentServiceMock) CreateShipment(shipment *models.Shipment, address *models.Address, lineItems []*models.ShipmentLineItem) (*models.Shipment, error) {
	args := service.Called(shipment, address, lineItems)

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

func (service *ShipmentServiceMock) DeleteShipment(id uint) error {
	args := service.Called(id)

	if args.Bool(0) {
		return nil
	}

	return args.Error(1)
}
