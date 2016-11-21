package services

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

type shippingMethodService struct {
	repository repositories.IShippingMethodRepository
}

type IShippingMethodService interface {
	GetShippingMethods() ([]*models.ShippingMethod, exceptions.IException)
	GetShippingMethodByID(id uint) (*models.ShippingMethod, exceptions.IException)
	CreateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, exceptions.IException)
	UpdateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, exceptions.IException)
	DeleteShippingMethod(id uint) exceptions.IException
}

func NewShippingMethodService(repository repositories.IShippingMethodRepository) IShippingMethodService {
	return &shippingMethodService{repository}
}

func (service *shippingMethodService) GetShippingMethods() ([]*models.ShippingMethod, exceptions.IException) {
	return service.repository.GetShippingMethods()
}

func (service *shippingMethodService) GetShippingMethodByID(id uint) (*models.ShippingMethod, exceptions.IException) {
	return service.repository.GetShippingMethodByID(id)
}

func (service *shippingMethodService) CreateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, exceptions.IException) {
	return service.repository.CreateShippingMethod(shippingMethod)
}

func (service *shippingMethodService) UpdateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, exceptions.IException) {
	return service.repository.UpdateShippingMethod(shippingMethod)
}

func (service *shippingMethodService) DeleteShippingMethod(id uint) exceptions.IException {
	return service.repository.DeleteShippingMethod(id)
}
