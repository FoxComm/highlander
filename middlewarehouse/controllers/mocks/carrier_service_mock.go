package mocks

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type CarrierServiceMock struct {
	mock.Mock
}

func (service *CarrierServiceMock) GetCarriers() ([]*models.Carrier, error) {
	args := service.Called()

	if models, ok := args.Get(0).([]*models.Carrier); ok {
		return models, nil
	}

	return nil, args.Error(1)
}

func (service *CarrierServiceMock) GetCarrierByID(id uint) (*models.Carrier, error) {
	args := service.Called()

	if model, ok := args.Get(0).(*models.Carrier); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *CarrierServiceMock) CreateCarrier(carrier *models.Carrier) (uint, error) {
	args := service.Called()

	if id, ok := args.Get(0).(uint); ok {
		return id, nil
	}

	return 0, args.Error(1)
}

func (service *CarrierServiceMock) UpdateCarrier(carrier *models.Carrier) error {
	args := service.Called()

	if args.Bool(0) {
		return nil
	}

	return args.Error(1)
}

func (service *CarrierServiceMock) DeleteCarrier(id uint) error {
	args := service.Called()

	if args.Bool(0) {
		return nil
	}

	return args.Error(1)
}
