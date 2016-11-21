package repositories

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/jinzhu/gorm"
)

const (
	ErrorCarrierNotFound = "Carrier with id=%d not found"
	carrierEntity = "carrier"
)

type carrierRepository struct {
	db *gorm.DB
}

type ICarrierRepository interface {
	GetCarriers() ([]*models.Carrier, exceptions.IException)
	GetCarrierByID(id uint) (*models.Carrier, exceptions.IException)
	CreateCarrier(carrier *models.Carrier) (*models.Carrier, exceptions.IException)
	UpdateCarrier(carrier *models.Carrier) (*models.Carrier, exceptions.IException)
	DeleteCarrier(id uint) exceptions.IException
}

func NewCarrierRepository(db *gorm.DB) ICarrierRepository {
	return &carrierRepository{db}
}

func (repository *carrierRepository) GetCarriers() ([]*models.Carrier, exceptions.IException) {
	var carriers []*models.Carrier

	err := repository.db.Find(&carriers).Error

	return carriers, NewDatabaseException(err)
}

func (repository *carrierRepository) GetCarrierByID(id uint) (*models.Carrier, exceptions.IException) {
	carrier := &models.Carrier{}

	if err := repository.db.First(carrier, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, NewEntityNotFound(carrierEntity, string(id), fmt.Errorf(ErrorCarrierNotFound, id))
		}

		return nil, NewDatabaseException(err)
	}

	return carrier, nil
}

func (repository *carrierRepository) CreateCarrier(carrier *models.Carrier) (*models.Carrier, exceptions.IException) {
	err := repository.db.Create(carrier).Error

	if err != nil {
		return nil, NewDatabaseException(err)
	}

	return repository.GetCarrierByID(carrier.ID)
}

func (repository *carrierRepository) UpdateCarrier(carrier *models.Carrier) (*models.Carrier, exceptions.IException) {
	result := repository.db.Model(&carrier).Updates(carrier)

	if result.Error != nil {
		return nil, NewDatabaseException(result.Error)
	}

	if result.RowsAffected == 0 {
		return nil, NewEntityNotFound(carrierEntity, string(carrier.ID), fmt.Errorf(ErrorCarrierNotFound, carrier.ID))
	}

	return repository.GetCarrierByID(carrier.ID)
}

func (repository *carrierRepository) DeleteCarrier(id uint) exceptions.IException {
	res := repository.db.Delete(&models.Carrier{}, id)

	if res.Error != nil {
		return NewDatabaseException(res.Error)
	}

	if res.RowsAffected == 0 {
		return NewEntityNotFound(carrierEntity, string(id), fmt.Errorf(ErrorCarrierNotFound, id))
	}

	return nil
}
