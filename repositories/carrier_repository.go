package repositories

import (
	"fmt"

	"github.com/FoxComm/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

const (
	ErrorCarrierNotFound = "Carrier with id=%d not found"
)

type carrierRepository struct {
	db *gorm.DB
}

type ICarrierRepository interface {
	GetCarriers() ([]*models.Carrier, error)
	GetCarrierByID(id uint) (*models.Carrier, error)
	CreateCarrier(carrier *models.Carrier) (*models.Carrier, error)
	UpdateCarrier(carrier *models.Carrier) (*models.Carrier, error)
	DeleteCarrier(id uint) error
}

func NewCarrierRepository(db *gorm.DB) ICarrierRepository {
	return &carrierRepository{db}
}

func (repository *carrierRepository) GetCarriers() ([]*models.Carrier, error) {
	var carriers []*models.Carrier

	err := repository.db.Find(&carriers).Error

	return carriers, err
}

func (repository *carrierRepository) GetCarrierByID(id uint) (*models.Carrier, error) {
	carrier := &models.Carrier{}

	if err := repository.db.First(carrier, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, fmt.Errorf(ErrorCarrierNotFound, id)
		}

		return nil, err
	}

	return carrier, nil
}

func (repository *carrierRepository) CreateCarrier(carrier *models.Carrier) (*models.Carrier, error) {
	err := repository.db.Create(carrier).Error

	if err != nil {
		return nil, err
	}

	return repository.GetCarrierByID(carrier.ID)
}

func (repository *carrierRepository) UpdateCarrier(carrier *models.Carrier) (*models.Carrier, error) {
	result := repository.db.Model(&carrier).Updates(carrier)

	if result.Error != nil {
		return nil, result.Error
	}

	if result.RowsAffected == 0 {
		return nil, fmt.Errorf(ErrorCarrierNotFound, carrier.ID)
	}

	return repository.GetCarrierByID(carrier.ID)
}

func (repository *carrierRepository) DeleteCarrier(id uint) error {
	res := repository.db.Delete(&models.Carrier{}, id)

	if res.Error != nil {
		return res.Error
	}

	if res.RowsAffected == 0 {
		return fmt.Errorf(ErrorCarrierNotFound, id)
	}

	return nil
}
