package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type StockItemUnitRepositoryMock struct {
	mock.Mock
}

func (service *StockItemUnitRepositoryMock) GetStockItemUnitIDs(stockItemID uint, unitStatus models.UnitStatus, unitType models.UnitType, count int) ([]uint, error) {
	args := service.Called(stockItemID, unitStatus, unitType, count)

	if models, ok := args.Get(0).([]uint); ok {
		return models, nil
	}

	return nil, args.Error(1)
}

func (service *StockItemUnitRepositoryMock) GetUnitsInOrder(refNum string) ([]*models.StockItemUnit, error) {
	args := service.Called(refNum)

	if models, ok := args.Get(0).([]*models.StockItemUnit); ok {
		return models, nil
	}

	return nil, args.Error(1)
}

func (service *StockItemUnitRepositoryMock) HoldUnitsInOrder(refNum string, ids []uint) (int, error) {
	args := service.Called(refNum, ids)

	if result, ok := args.Get(0).(int); ok {
		return result, nil
	}

	return 0, args.Error(1)
}

func (service *StockItemUnitRepositoryMock) ReserveUnitsInOrder(refNum string) (int, error) {
	args := service.Called(refNum)

	if result, ok := args.Get(0).(int); ok {
		return result, nil
	}

	return 0, args.Error(1)
}

func (service *StockItemUnitRepositoryMock) UnsetUnitsInOrder(refNum string) (int, error) {
	args := service.Called(refNum)

	if result, ok := args.Get(0).(int); ok {
		return result, nil
	}

	return 0, args.Error(1)
}

func (service *StockItemUnitRepositoryMock) ShipUnitsInOrder(refNum string) (int, error) {
	args := service.Called(refNum)

	if result, ok := args.Get(0).(int); ok {
		return result, nil
	}

	return 0, args.Error(1)
}

func (service *StockItemUnitRepositoryMock) DeleteUnitsInOrder(refNum string) (int, error) {
	args := service.Called(refNum)

	if result, ok := args.Get(0).(int); ok {
		return result, nil
	}

	return 0, args.Error(1)
}

func (service *StockItemUnitRepositoryMock) GetReleaseQtyByRefNum(refNum string) ([]*models.Release, error) {
	args := service.Called(refNum)

	if models, ok := args.Get(0).([]*models.Release); ok {
		return models, nil
	}

	return nil, args.Error(1)
}

func (service *StockItemUnitRepositoryMock) CreateUnits(units []*models.StockItemUnit) error {
	args := service.Called(units)

	return args.Error(0)
}

func (service *StockItemUnitRepositoryMock) DeleteUnits(ids []uint) error {
	args := service.Called(ids)

	return args.Error(0)
}
