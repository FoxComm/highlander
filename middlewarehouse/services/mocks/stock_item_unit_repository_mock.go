package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type StockItemUnitRepositoryMock struct {
	mock.Mock
}

func (service *StockItemUnitRepositoryMock) GetStockItemUnitIDs(stockItemID uint, unitStatus models.UnitStatus, unitType models.UnitType, count int) ([]uint, exceptions.IException) {
	args := service.Called(stockItemID, unitStatus, unitType, count)

	if models, ok := args.Get(0).([]uint); ok {
		return models, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *StockItemUnitRepositoryMock) GetUnitsInOrder(refNum string) ([]*models.StockItemUnit, exceptions.IException) {
	args := service.Called(refNum)

	if models, ok := args.Get(0).([]*models.StockItemUnit); ok {
		return models, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *StockItemUnitRepositoryMock) HoldUnitsInOrder(refNum string, ids []uint) (int, exceptions.IException) {
	args := service.Called(refNum, ids)

	if result, ok := args.Get(0).(int); ok {
		return result, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return 0, ex
	}

	return 0, nil
}

func (service *StockItemUnitRepositoryMock) ReserveUnitsInOrder(refNum string) (int, exceptions.IException) {
	args := service.Called(refNum)

	if result, ok := args.Get(0).(int); ok {
		return result, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return 0, ex
	}

	return 0, nil
}

func (service *StockItemUnitRepositoryMock) UnsetUnitsInOrder(refNum string) (int, exceptions.IException) {
	args := service.Called(refNum)

	if result, ok := args.Get(0).(int); ok {
		return result, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return 0, ex
	}

	return 0, nil
}

func (service *StockItemUnitRepositoryMock) GetReleaseQtyByRefNum(refNum string) ([]*models.Release, exceptions.IException) {
	args := service.Called(refNum)

	if models, ok := args.Get(0).([]*models.Release); ok {
		return models, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *StockItemUnitRepositoryMock) CreateUnits(units []*models.StockItemUnit) exceptions.IException {
	args := service.Called(units)

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return ex
	}

	return nil
}

func (service *StockItemUnitRepositoryMock) DeleteUnits(ids []uint) exceptions.IException {
	args := service.Called(ids)

	if ex, ok := args.Get(0).(exceptions.IException); ok {
		return ex
	}

	return nil
}
