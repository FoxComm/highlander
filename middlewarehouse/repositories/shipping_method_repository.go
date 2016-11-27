package repositories

import (
	"fmt"
	"strconv"

	"github.com/FoxComm/highlander/middlewarehouse/common/db"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

const (
	ErrorShippingMethodNotFound = "Shipping method with id=%d not found"
	ShippingMethodEntity        = "shippingMethod"
)

type shippingMethodRepository struct {
	db *gorm.DB
}

type IShippingMethodRepository interface {
	GetShippingMethods() ([]*models.ShippingMethod, exceptions.IException)
	GetShippingMethodByID(id uint) (*models.ShippingMethod, exceptions.IException)
	CreateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, exceptions.IException)
	UpdateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, exceptions.IException)
	DeleteShippingMethod(id uint) exceptions.IException
}

func NewShippingMethodRepository(db *gorm.DB) IShippingMethodRepository {
	return &shippingMethodRepository{db}
}

func (repository *shippingMethodRepository) GetShippingMethods() ([]*models.ShippingMethod, exceptions.IException) {
	var shippingMethods []*models.ShippingMethod

	err := repository.db.Preload("Carrier").Find(&shippingMethods).Error

	return shippingMethods, db.NewDatabaseException(err)
}

func (repository *shippingMethodRepository) GetShippingMethodByID(id uint) (*models.ShippingMethod, exceptions.IException) {
	shippingMethod := &models.ShippingMethod{}

	if err := repository.db.First(shippingMethod, id).Related(&shippingMethod.Carrier).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, NewEntityNotFoundException(ShippingMethodEntity, strconv.Itoa(int(id)), fmt.Errorf(ErrorShippingMethodNotFound, id))
		}

		return nil, db.NewDatabaseException(err)
	}

	return shippingMethod, nil
}

func (repository *shippingMethodRepository) CreateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, exceptions.IException) {
	err := repository.db.Set("gorm:save_associations", false).Save(shippingMethod).Error

	if err != nil {
		return nil, db.NewDatabaseException(err)
	}

	return repository.GetShippingMethodByID(shippingMethod.ID)
}

func (repository *shippingMethodRepository) UpdateShippingMethod(shippingMethod *models.ShippingMethod) (*models.ShippingMethod, exceptions.IException) {
	result := repository.db.Set("gorm:save_associations", false).Save(shippingMethod)

	if result.Error != nil {
		return nil, db.NewDatabaseException(result.Error)
	}

	if result.RowsAffected == 0 {
		return nil, NewEntityNotFoundException(ShippingMethodEntity, strconv.Itoa(int(shippingMethod.ID)), fmt.Errorf(ErrorShippingMethodNotFound, shippingMethod.ID))
	}

	return repository.GetShippingMethodByID(shippingMethod.ID)
}

func (repository *shippingMethodRepository) DeleteShippingMethod(id uint) exceptions.IException {
	res := repository.db.Delete(&models.ShippingMethod{}, id)

	if res.Error != nil {
		return db.NewDatabaseException(res.Error)
	}

	if res.RowsAffected == 0 {
		return NewEntityNotFoundException(ShippingMethodEntity, strconv.Itoa(int(id)), fmt.Errorf(ErrorShippingMethodNotFound, id))
	}

	return nil
}
