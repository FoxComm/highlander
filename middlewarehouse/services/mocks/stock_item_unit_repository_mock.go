package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/mock"
)

type StockItemUnitRepositoryMock struct {
	mock.Mock
}

// WithTransaction returns a shallow copy of repository with its db changed to txn. The provided txn must be non-nil.
func (repository *StockItemUnitRepositoryMock) WithTransaction(txn *gorm.DB) repositories.IStockItemUnitRepository {
	if txn == nil {
		panic("nil transaction")
	}

	return repository
}

func (repository *StockItemUnitRepositoryMock) GetStockItemUnitIDs(stockItemID uint, unitStatus models.UnitStatus, unitType models.UnitType, count int) ([]uint, error) {
	args := repository.Called(stockItemID, unitStatus, unitType, count)

	if models, ok := args.Get(0).([]uint); ok {
		return models, nil
	}

	return nil, args.Error(1)
}

func (repository *StockItemUnitRepositoryMock) GetUnitsInOrder(refNum string) ([]*models.StockItemUnit, error) {
	args := repository.Called(refNum)

	if models, ok := args.Get(0).([]*models.StockItemUnit); ok {
		return models, nil
	}

	return nil, args.Error(1)
}

func (repository *StockItemUnitRepositoryMock) HoldUnitsInOrder(refNum string, ids []uint) (int, error) {
	args := repository.Called(refNum, ids)

	if result, ok := args.Get(0).(int); ok {
		return result, nil
	}

	return 0, args.Error(1)
}

func (repository *StockItemUnitRepositoryMock) ReserveUnitsInOrder(refNum string) (int, error) {
	args := repository.Called(refNum)

	if result, ok := args.Get(0).(int); ok {
		return result, nil
	}

	return 0, args.Error(1)
}

func (repository *StockItemUnitRepositoryMock) UnsetUnitsInOrder(refNum string) (int, error) {
	args := repository.Called(refNum)

	if result, ok := args.Get(0).(int); ok {
		return result, nil
	}

	return 0, args.Error(1)
}

func (repository *StockItemUnitRepositoryMock) ShipUnitsInOrder(refNum string) (int, error) {
	args := repository.Called(refNum)

	if result, ok := args.Get(0).(int); ok {
		return result, nil
	}

	return 0, args.Error(1)
}

func (repository *StockItemUnitRepositoryMock) DeleteUnitsInOrder(refNum string) (int, error) {
	args := repository.Called(refNum)

	if result, ok := args.Get(0).(int); ok {
		return result, nil
	}

	return 0, args.Error(1)
}

func (repository *StockItemUnitRepositoryMock) GetQtyForOrder(refNum string) ([]*models.Release, error) {
	args := repository.Called(refNum)

	if models, ok := args.Get(0).([]*models.Release); ok {
		return models, nil
	}

	return nil, args.Error(1)
}

func (repository *StockItemUnitRepositoryMock) GetUnitForLineItem(refNum string, sku string) (*models.StockItemUnit, error) {
	args := repository.Called(refNum, sku)

	if result, ok := args.Get(0).(*models.StockItemUnit); ok {
		return result, nil
	}

	return nil, args.Error(1)
}

func (repository *StockItemUnitRepositoryMock) ReserveUnit(orderRefNum string, skuCode string) (*models.StockItemUnit, error) {
	args := repository.Called(orderRefNum, skuCode)

	if result, ok := args.Get(0).(*models.StockItemUnit); ok {
		return result, nil
	}

	return nil, args.Error(1)
}

func (repository *StockItemUnitRepositoryMock) CreateUnits(units []*models.StockItemUnit) error {
	args := repository.Called(units)

	return args.Error(0)
}

func (repository *StockItemUnitRepositoryMock) DeleteUnits(ids []uint) error {
	args := repository.Called(ids)

	return args.Error(0)
}
