package repositories

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

const (
	ErrorShippingMethodNotFound = "Shipping method with id=%d not found"
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

	err := repository.db.Preload("Carrier").Preload("ExternalFreight").Find(&shippingMethods).Error

	return shippingMethods, err
}

func (repository *shippingMethodRepository) GetShippingMethodByID(id uint) (*models.ShippingMethod, error) {
	shippingMethod := &models.ShippingMethod{}
	err := repository.db.
		Preload("Carrier").
		Preload("ExternalFreight").
		First(shippingMethod, id).Error

	if err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, fmt.Errorf(ErrorShippingMethodNotFound, id)
		}

		return nil, err
	}

	return shippingMethod, nil
}

func (repository *shippingMethodRepository) CreateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error) {
	err := repository.db.Set("gorm:save_associations", false).Save(shippingMethod).Error

	if err != nil {
		return nil, err
	}

	return repository.GetShippingMethodByID(shippingMethod.ID)
}

func (repository *shippingMethodRepository) UpdateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, error) {
	result := repository.db.Set("gorm:save_associations", false).Save(shippingMethod)

	if result.Error != nil {
		return nil, result.Error
	}

	if result.RowsAffected == 0 {
		return nil, fmt.Errorf(ErrorShippingMethodNotFound, shippingMethod.ID)
	}

	return repository.GetShippingMethodByID(shippingMethod.ID)
}

func (repository *shippingMethodRepository) DeleteShippingMethod(id uint) error {
	res := repository.db.Delete(&models.ShippingMethod{}, id)

	if res.Error != nil {
		return res.Error
	}

	if res.RowsAffected == 0 {
		return fmt.Errorf(ErrorShippingMethodNotFound, id)
	}

	return nil
}
