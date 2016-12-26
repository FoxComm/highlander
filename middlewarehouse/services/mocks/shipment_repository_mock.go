package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
	"github.com/jinzhu/gorm"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
)

type ShipmentRepositoryMock struct {
	mock.Mock
}

// WithTransaction returns a shallow copy of repository with its db changed to txn. The provided txn must be non-nil.
func (repository *ShipmentRepositoryMock) WithTransaction(txn *gorm.DB) repositories.IShipmentRepository {
	if txn == nil {
		panic("nil transaction")
	}

	return repository
}

func (repository *ShipmentRepositoryMock) GetShipmentsByOrder(referenceNumber string) ([]*models.Shipment, error) {
	args := repository.Called(referenceNumber)

	if model, ok := args.Get(0).([]*models.Shipment); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (repository *ShipmentRepositoryMock) GetShipmentByID(id uint) (*models.Shipment, error) {
	args := repository.Called(id)

	if model, ok := args.Get(0).(*models.Shipment); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (repository *ShipmentRepositoryMock) CreateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	args := repository.Called(shipment)

	if model, ok := args.Get(0).(*models.Shipment); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (repository *ShipmentRepositoryMock) UpdateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	args := repository.Called(shipment)

	if model, ok := args.Get(0).(*models.Shipment); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (repository *ShipmentRepositoryMock) DeleteShipment(id uint) error {
	args := repository.Called(id)

	return args.Error(0)
}
