package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type ShippingMethodServiceMock struct {
	mock.Mock
}

func (service *ShippingMethodServiceMock) GetShippingMethods() ([]*models.ShippingMethod, exceptions.IException) {
	args := service.Called()

	if models, ok := args.Get(0).([]*models.ShippingMethod); ok {
		return models, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShippingMethodServiceMock) GetShippingMethodByID(id uint) (*models.ShippingMethod, exceptions.IException) {
	args := service.Called(id)

	if model, ok := args.Get(0).(*models.ShippingMethod); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShippingMethodServiceMock) CreateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, exceptions.IException) {
	args := service.Called(shippingMethod)

	if model, ok := args.Get(0).(*models.ShippingMethod); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShippingMethodServiceMock) UpdateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, exceptions.IException) {
	args := service.Called(shippingMethod)

	if model, ok := args.Get(0).(*models.ShippingMethod); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (service *ShippingMethodServiceMock) DeleteShippingMethod(id uint) exceptions.IException {
	args := service.Called(id)

	if ex, ok := args.Get(0).(exceptions.IException); ok {
		return ex
	}

	return nil
}
