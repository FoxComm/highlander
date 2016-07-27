package services

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

type shippingMethodService struct {
	db *gorm.DB
}

type IShippingMethodService interface {
	GetShippingMethods() ([]*models.ShippingMethod, error)
	GetShippingMethodByID(id uint) (*models.ShippingMethod, error)
	CreateShippingMethod(shippingMethod *models.ShippingMethod) (uint, error)
	UpdateShippingMethod(shippingMethod *models.ShippingMethod) error
	DeleteShippingMethod(id uint) error
}

func NewShippingMethodService(db *gorm.DB) IShippingMethodService {
	return &shippingMethodService{db}
}

func (service *shippingMethodService) GetShippingMethods() ([]*models.ShippingMethod, error) {
	var data []models.ShippingMethod
	if err := service.db.Find(&data).Error; err != nil {
		return nil, err
	}

	shippingMethods := make([]*models.ShippingMethod, len(data))
	for i := range data {
		shippingMethods[i] = &data[i]
	}

	return shippingMethods, nil
}

func (service *shippingMethodService) GetShippingMethodByID(id uint) (*models.ShippingMethod, error) {
	var shippingMethod models.ShippingMethod
	if err := service.db.First(&shippingMethod, id).Error; err != nil {
		return nil, err
	}

	return &shippingMethod, nil
}

func (service *shippingMethodService) CreateShippingMethod(shippingMethod *models.ShippingMethod) (uint, error) {
	err := service.db.Create(shippingMethod).Error

	return shippingMethod.ID, err
}

func (service *shippingMethodService) UpdateShippingMethod(shippingMethod *models.ShippingMethod) error {
	return service.db.Model(&shippingMethod).Updates(shippingMethod).Error
}

func (service *shippingMethodService) DeleteShippingMethod(id uint) error {
	shippingMethod := models.ShippingMethod{ID: id}

	return service.db.Delete(&shippingMethod).Error
}
