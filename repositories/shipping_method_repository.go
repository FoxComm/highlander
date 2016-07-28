package repositories

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

type shippingMethodRepository struct {
	db *gorm.DB
}

type IShippingMethodRepository interface {
	GetShippingMethods() ([]*models.ShippingMethod, error)
	GetShippingMethodByID(id uint) (*models.ShippingMethod, error)
	CreateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error)
	UpdateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error)
	DeleteShippingMethod(id uint) error
}

func NewShippingMethodRepository(db *gorm.DB) IShippingMethodRepository {
	return &shippingMethodRepository{db}
}

func (repository *shippingMethodRepository) GetShippingMethods() ([]*models.ShippingMethod, error) {
	var shippingMethods []*models.ShippingMethod
	if err := repository.db.Find(&shippingMethods).Error; err != nil {
		return nil, err
	}

	return shippingMethods, nil
}

func (repository *shippingMethodRepository) GetShippingMethodByID(id uint) (*models.ShippingMethod, error) {
	var shippingMethod models.ShippingMethod
	if err := repository.db.First(&shippingMethod, id).Error; err != nil {
		return nil, err
	}

	return &shippingMethod, nil
}

func (repository *shippingMethodRepository) CreateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error) {
	err := repository.db.Create(shippingMethod).Error

	return shippingMethod, err
}

func (repository *shippingMethodRepository) UpdateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error) {
	result := repository.db.Model(shippingMethod).Updates(shippingMethod)

	if result.RowsAffected == 0 {
		return shippingMethod, gorm.ErrRecordNotFound
	}

	return shippingMethod, result.Error
}

func (repository *shippingMethodRepository) DeleteShippingMethod(id uint) error {
	shippingMethod := models.ShippingMethod{ID: id}

	result := repository.db.Delete(&shippingMethod)

	if result.RowsAffected == 0 {
		return gorm.ErrRecordNotFound
	}

	return result.Error
}
