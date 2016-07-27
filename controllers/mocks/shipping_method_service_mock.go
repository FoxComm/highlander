package mocks

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type ShippingMethodServiceMock struct {
	mock.Mock
}

func (service *ShippingMethodServiceMock) GetShippingMethods() ([]*models.ShippingMethod, error) {
	args := service.Called()

	if models, ok := args.Get(0).([]*models.ShippingMethod); ok {
		return models, nil
	}

	return nil, args.Error(1)
}

func (service *ShippingMethodServiceMock) GetShippingMethodByID(id uint) (*models.ShippingMethod, error) {
	args := service.Called()

	if model, ok := args.Get(0).(*models.ShippingMethod); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShippingMethodServiceMock) CreateShippingMethod(shippingMethod *models.ShippingMethod) (uint, error) {
	args := service.Called()

	if id, ok := args.Get(0).(uint); ok {
		return id, nil
	}

	return 0, args.Error(1)
}

func (service *ShippingMethodServiceMock) UpdateShippingMethod(shippingMethod *models.ShippingMethod) error {
	args := service.Called()

	if args.Bool(0) {
		return nil
	}

	return args.Error(1)
}

func (service *ShippingMethodServiceMock) DeleteShippingMethod(id uint) error {
	args := service.Called()

	if args.Bool(0) {
		return nil
	}

	return args.Error(1)
}
