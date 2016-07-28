package mocks

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type ShippingMethodRepositoryMock struct {
	mock.Mock
}

func (service *ShippingMethodRepositoryMock) GetShippingMethods() ([]*models.ShippingMethod, error) {
	args := service.Called()

	if models, ok := args.Get(0).([]*models.ShippingMethod); ok {
		return models, nil
	}

	return nil, args.Error(1)
}

func (service *ShippingMethodRepositoryMock) GetShippingMethodByID(id uint) (*models.ShippingMethod, error) {
	args := service.Called()

	if model, ok := args.Get(0).(*models.ShippingMethod); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShippingMethodRepositoryMock) CreateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error) {
	args := service.Called()

	if model, ok := args.Get(0).(*models.ShippingMethod); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShippingMethodRepositoryMock) UpdateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error) {
	args := service.Called()

	if model, ok := args.Get(0).(*models.ShippingMethod); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShippingMethodRepositoryMock) DeleteShippingMethod(id uint) error {
	args := service.Called()

	if args.Bool(0) {
		return nil
	}

	return args.Error(1)
}
