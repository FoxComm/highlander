package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/models"

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
	args := service.Called(id)

	if model, ok := args.Get(0).(*models.ShippingMethod); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShippingMethodServiceMock) CreateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error) {
	args := service.Called(shippingMethod)

	if model, ok := args.Get(0).(*models.ShippingMethod); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShippingMethodServiceMock) UpdateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error) {
	args := service.Called(shippingMethod)

	if model, ok := args.Get(0).(*models.ShippingMethod); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (service *ShippingMethodServiceMock) DeleteShippingMethod(id uint) error {
	args := service.Called(id)

	return args.Error(0)
}

func (service *ShippingMethodServiceMock) EvaluateForOrder(order *payloads.Order) ([]*responses.OrderShippingMethod, error) {
	args := service.Called(order)

	resp := []*responses.OrderShippingMethod{}
	return resp, args.Error(0)
}
