package services

import (
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/repositories"
)

type shippingMethodService struct {
	repository repositories.IShippingMethodRepository
}

type IShippingMethodService interface {
	GetShippingMethods() ([]*models.ShippingMethod, error)
	GetShippingMethodByID(id uint) (*models.ShippingMethod, error)
	CreateShippingMethod(shippingMethod *models.ShippingMethod) (uint, error)
	UpdateShippingMethod(shippingMethod *models.ShippingMethod) error
	DeleteShippingMethod(id uint) error
}

func NewShippingMethodService(repository repositories.IShippingMethodRepository) IShippingMethodService {
	return &shippingMethodService{repository}
}

func (service *shippingMethodService) GetShippingMethods() ([]*models.ShippingMethod, error) {
	return service.repository.GetShippingMethods()
}

func (service *shippingMethodService) GetShippingMethodByID(id uint) (*models.ShippingMethod, error) {
	return service.repository.GetShippingMethodByID(id)
}

func (service *shippingMethodService) CreateShippingMethod(shippingMethod *models.ShippingMethod) (uint, error) {
	return service.repository.CreateShippingMethod(shippingMethod)
}

func (service *shippingMethodService) UpdateShippingMethod(shippingMethod *models.ShippingMethod) error {
	return service.repository.UpdateShippingMethod(shippingMethod)
}

func (service *shippingMethodService) DeleteShippingMethod(id uint) error {
	return service.repository.DeleteShippingMethod(id)
}
