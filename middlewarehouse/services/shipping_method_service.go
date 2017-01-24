package services

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/jinzhu/gorm"
)

type shippingMethodService struct {
	repository repositories.IShippingMethodRepository
}

type ShippingMethodService interface {
	GetShippingMethods() ([]*models.ShippingMethod, error)
	GetShippingMethodByID(id uint) (*models.ShippingMethod, error)
	CreateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error)
	UpdateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error)
	DeleteShippingMethod(id uint) error
}

func NewShippingMethodService(db *gorm.DB) ShippingMethodService {
	repository := repositories.NewShippingMethodRepository(db)
	return &shippingMethodService{repository}
}

func (service *shippingMethodService) GetShippingMethods() ([]*models.ShippingMethod, error) {
	return service.repository.GetShippingMethods()
}

func (service *shippingMethodService) GetShippingMethodByID(id uint) (*models.ShippingMethod, error) {
	return service.repository.GetShippingMethodByID(id)
}

func (service *shippingMethodService) CreateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error) {
	return service.repository.CreateShippingMethod(shippingMethod)
}

func (service *shippingMethodService) UpdateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error) {
	return service.repository.UpdateShippingMethod(shippingMethod)
}

func (service *shippingMethodService) DeleteShippingMethod(id uint) error {
	return service.repository.DeleteShippingMethod(id)
}
