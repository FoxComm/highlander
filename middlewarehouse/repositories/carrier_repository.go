package repositories

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/failures"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

const (
	ErrorCarrierNotFound = "Carrier with id=%d not found"
)

type carrierRepository struct {
	db *gorm.DB
}

type ICarrierRepository interface {
	GetCarriers() ([]*models.Carrier, failures.Failure)
	GetCarrierByID(id uint) (*models.Carrier, failures.Failure)
	CreateCarrier(carrier *models.Carrier) failures.Failure
	UpdateCarrier(carrier *models.Carrier) failures.Failure
	DeleteCarrier(id uint) failures.Failure
}

func NewCarrierRepository(db *gorm.DB) ICarrierRepository {
	return &carrierRepository{db}
}

func (repository *carrierRepository) GetCarriers() ([]*models.Carrier, failures.Failure) {
	var carriers []*models.Carrier

	err := repository.db.Find(&carriers).Error

	return carriers, failures.NewFailure(err)
}

func (repository *carrierRepository) GetCarrierByID(id uint) (*models.Carrier, failures.Failure) {
	carrier := &models.Carrier{}

	if err := repository.db.First(carrier, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, failures.NewNotFound("Carrier", id)
		}

		return nil, failures.NewFailure(err)
	}

	return carrier, nil
}

func (repository *carrierRepository) CreateCarrier(carrier *models.Carrier) failures.Failure {
	if err := repository.db.Create(carrier).Error; err != nil {
		return failures.NewFailure(err)
	}

	return nil
}

func (repository *carrierRepository) UpdateCarrier(carrier *models.Carrier) failures.Failure {
	result := repository.db.Model(&carrier).Updates(carrier)

	if result.Error != nil {
		return failures.NewFailure(result.Error)
	}

	if result.RowsAffected == 0 {
		return failures.NewNotFound("Carrier", carrier.ID)
	}

	return nil
}

func (repository *carrierRepository) DeleteCarrier(id uint) failures.Failure {
	res := repository.db.Delete(&models.Carrier{}, id)

	if res.Error != nil {
		return failures.NewFailure(res.Error)
	}

	if res.RowsAffected == 0 {
		return failures.NewNotFound("Carrier", id)
	}

	return nil
}
