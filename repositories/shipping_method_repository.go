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

	err := repository.db.Find(&shippingMethods).Error

	return shippingMethods, err
}

func (repository *shippingMethodRepository) GetShippingMethodByID(id uint) (*models.ShippingMethod, error) {
	shippingMethod :=&models.ShippingMethod{}

	if err := repository.db.First(shippingMethod, id).Error; err != nil {
		return nil, err
	}

	return shippingMethod, nil
}

func (repository *shippingMethodRepository) CreateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error) {
	err := repository.db.Create(shippingMethod).Error

	if err != nil {
		return nil, err
	}

	return repository.GetShippingMethodByID(shippingMethod.ID)
}

func (repository *shippingMethodRepository) UpdateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error) {
	result := repository.db.Model(shippingMethod).Updates(shippingMethod)

	if result.Error != nil {
		return nil, result.Error
	}

	if result.RowsAffected == 0 {
		return nil, gorm.ErrRecordNotFound
	}

	return repository.GetShippingMethodByID(shippingMethod.ID)
}

func (repository *shippingMethodRepository) DeleteShippingMethod(id uint) error {
	res := repository.db.Delete(&models.ShippingMethod{}, id)

	if res.Error != nil {
		return res.Error
	}

	if res.RowsAffected == 0 {
		return gorm.ErrRecordNotFound
	}

	return nil
}
